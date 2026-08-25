package com.gentry.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台级权限隔离 —— 租户管理员越权的回归测试。
 *
 * <p><b>缺陷现场</b>：新建租户的 admin 能列出/新增/修改/删除<b>所有</b>租户，包括默认租户。
 * 两个独立成因：</p>
 *
 * <ul>
 *   <li><b>D-1</b> {@code TenantServiceImpl.create} 把<b>全部</b>菜单给了新租户的 ADMIN 角色</li>
 *   <li><b>D-2</b> {@code RoleServiceImpl.assignMenus} 只校验 menuId 存不存在，不校验调用者
 *       有没有权分配它 —— 租户管理员握有 {@code system:role:assignMenu}，能自己勾回来</li>
 * </ul>
 *
 * <p><b>为什么以前没测出来</b>：种子数据里的 ADMIN（{@code role_id=1}）是对的，70 个菜单里
 * 只有 61 个，恰好不含租户管理；E2E 的「ADMIN 无租户管理权限」测的就是它，一直绿。
 * 而「建租户」这条路径给出的权限完全不同 —— 两条路径结果不一致，测试只覆盖了对的那条。
 * 所以本类的每条用例都<b>先建一个租户</b>，走的是原来没人走的那条路。</p>
 */
@DisplayName("平台级权限隔离 - 租户管理员不得越权")
class PlatformPermissionIsolationApiIT extends BaseApiIT {

    private static final AtomicInteger SEQ = new AtomicInteger();
    /** 平台级权限点的前缀（租户管理）；Redis 的两个破坏性操作另算 */
    private static final String TENANT_PERM_PREFIX = "system:tenant";

    /** 建一个租户，返回 {@code [tenantCode, adminUsername, adminPassword]} */
    private String[] createTenant() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "iso" + SEQ.incrementAndGet() + "abc");   // 6-20 位字母数字
        body.put("name", "隔离回归租户");
        body.put("contact", "回归");
        MvcResult r = mockMvc.perform(superPost("/api/v1/tenants").content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = parse(r).at("/data");
        return new String[]{
                data.path("tenantCode").asText(),
                data.path("adminUsername").asText(),
                data.path("adminPassword").asText()};
    }

    /** 递归收集菜单树里 {@code isPlatform == 1} 的节点 ID */
    private void collectPlatformMenuIds(JsonNode nodes, List<Long> out) {
        for (JsonNode n : nodes) {
            if (n.path("isPlatform").asInt(0) == 1) {
                out.add(n.path("id").asLong());
            }
            if (n.has("children") && n.get("children").isArray()) {
                collectPlatformMenuIds(n.get("children"), out);
            }
        }
    }

    private List<Long> platformMenuIds() throws Exception {
        MvcResult r = mockMvc.perform(superGet("/api/v1/menus"))
                .andExpect(status().isOk())
                .andReturn();
        List<Long> ids = new ArrayList<>();
        collectPlatformMenuIds(parse(r).at("/data"), ids);
        return ids;
    }

    // ==================== 契约：is_platform 标记本身 ====================

    @Test
    @DisplayName("菜单树下发isPlatform_且租户管理被标为平台级")
    void 菜单树下发isPlatform_且租户管理被标为平台级() throws Exception {
        MvcResult r = mockMvc.perform(superGet("/api/v1/menus"))
                .andExpect(status().isOk())
                .andReturn();

        List<Long> platform = new ArrayList<>();
        collectPlatformMenuIds(parse(r).at("/data"), platform);
        assertThat(platform)
                .as("V14 应把租户管理与 Redis 破坏性操作标成平台级；为空说明字段没下发或迁移没跑")
                .isNotEmpty();

        // 前端要靠这个字段隐藏节点，字段缺失会静默退化成「全都显示」
        assertThat(parse(r).at("/data").get(0).has("isPlatform"))
                .as("MenuTreeVO 必须下发 isPlatform，否则前端无法隐藏平台级权限点")
                .isTrue();
    }

    // ==================== D-1：新建租户的权限基线 ====================

    @Test
    @DisplayName("D1_新建租户的admin拿不到租户管理权限")
    void D1_新建租户的admin拿不到租户管理权限() throws Exception {
        String[] t = createTenant();
        String tenantAuth = login(t[0], t[1], t[2]);

        MvcResult r = mockMvc.perform(bareGet("/api/v1/auth/user-info").header("Authorization", tenantAuth))
                .andExpect(status().isOk())
                .andReturn();

        List<String> perms = new ArrayList<>();
        for (JsonNode p : parse(r).at("/data/permissions")) {
            perms.add(p.asText());
        }
        assertThat(perms)
                .as("新租户 admin 应当有租户级权限（不是空角色）")
                .isNotEmpty();
        assertThat(perms.stream().filter(p -> p.startsWith(TENANT_PERM_PREFIX)).toList())
                .as("租户管理是平台级权限，新租户的 admin 不该有 —— 有就能管理所有租户")
                .isEmpty();
        assertThat(perms)
                .as("Redis 的破坏性操作也是平台级（Redis 是所有租户共用一个实例）")
                .doesNotContain("monitor:redis:key:delete", "monitor:redis:slowlog:reset")
                // 只读的仍应保留：租户管理员可以看共享基础设施，不能改它
                .contains("monitor:redis:info");
    }

    @Test
    @DisplayName("D1_新建租户的admin访问租户列表返回403")
    void D1_新建租户的admin访问租户列表返回403() throws Exception {
        String[] t = createTenant();
        String tenantAuth = login(t[0], t[1], t[2]);

        mockMvc.perform(bareGet("/api/v1/tenants?pageNum=1&pageSize=10")
                        .header("Authorization", tenantAuth))
                .andExpect(status().isForbidden());
    }

    // ==================== D-2：自提权路径 ====================

    @Test
    @DisplayName("D2_租户管理员给角色分配平台级菜单被拒_40004")
    void D2_租户管理员给角色分配平台级菜单被拒_40004() throws Exception {
        String[] t = createTenant();
        String tenantAuth = login(t[0], t[1], t[2]);
        List<Long> platform = platformMenuIds();
        assertThat(platform).as("需要至少一个平台级菜单才能测").isNotEmpty();

        // 租户管理员在自己租户里建一个角色（它有 system:role:add）
        Map<String, Object> roleBody = new HashMap<>();
        roleBody.put("roleCode", "escalate" + SEQ.incrementAndGet());
        roleBody.put("roleName", "提权尝试");
        roleBody.put("sort", 1);
        MvcResult created = mockMvc.perform(barePost("/api/v1/roles")
                        .header("Authorization", tenantAuth)
                        .content(json(roleBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long roleId = parse(created).at("/data/id").asLong();

        /*
         * 关键断言：这就是「只修 D-1 是安全剧场」的证据 —— 即使新建时没给平台级权限，
         * 租户管理员也能走这条路自己勾回来。40004 = PLATFORM_MENU_FORBIDDEN。
         */
        mockMvc.perform(barePut("/api/v1/roles/{id}/menus", roleId)
                        .header("Authorization", tenantAuth)
                        .content(json(Map.of("menuIds", platform))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    @DisplayName("D2_租户管理员分配纯租户级菜单仍然成功_守卫不过度拦截")
    void D2_租户管理员分配纯租户级菜单仍然成功_守卫不过度拦截() throws Exception {
        String[] t = createTenant();
        String tenantAuth = login(t[0], t[1], t[2]);

        // 从新租户 admin 自己的角色里取一批菜单 —— 它们必然都是租户级的
        MvcResult info = mockMvc.perform(bareGet("/api/v1/auth/user-info").header("Authorization", tenantAuth))
                .andExpect(status().isOk())
                .andReturn();
        long ownRoleId = parse(info).at("/data/roles").get(0).path("id").asLong();
        MvcResult own = mockMvc.perform(bareGet("/api/v1/roles/{id}", ownRoleId)
                        .header("Authorization", tenantAuth))
                .andExpect(status().isOk())
                .andReturn();
        List<Long> tenantScoped = new ArrayList<>();
        for (JsonNode m : parse(own).at("/data/menuIds")) {
            tenantScoped.add(m.asLong());
        }
        assertThat(tenantScoped).as("新租户 admin 的角色应有菜单").isNotEmpty();

        Map<String, Object> roleBody = new HashMap<>();
        roleBody.put("roleCode", "normal" + SEQ.incrementAndGet());
        roleBody.put("roleName", "正常角色");
        roleBody.put("sort", 1);
        MvcResult created = mockMvc.perform(barePost("/api/v1/roles")
                        .header("Authorization", tenantAuth)
                        .content(json(roleBody)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long roleId = parse(created).at("/data/id").asLong();

        mockMvc.perform(barePut("/api/v1/roles/{id}/menus", roleId)
                        .header("Authorization", tenantAuth)
                        .content(json(Map.of("menuIds", tenantScoped))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("D2_平台超管分配平台级菜单不受限")
    void D2_平台超管分配平台级菜单不受限() throws Exception {
        List<Long> platform = platformMenuIds();
        assertThat(platform).isNotEmpty();

        Map<String, Object> roleBody = new HashMap<>();
        roleBody.put("roleCode", "platrole" + SEQ.incrementAndGet());
        roleBody.put("roleName", "平台角色");
        roleBody.put("sort", 1);
        MvcResult created = mockMvc.perform(superPost("/api/v1/roles").content(json(roleBody)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long roleId = parse(created).at("/data/id").asLong();

        // 守卫的判据是 UserContext.isPlatformAdmin()，超管应当不受限
        mockMvc.perform(superPut("/api/v1/roles/{id}/menus", roleId)
                        .content(json(Map.of("menuIds", platform))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 不回退：种子 ADMIN 的既有行为 ====================

    @Test
    @DisplayName("种子ADMIN仍无租户管理权限_原有约束不回退")
    void 种子ADMIN仍无租户管理权限_原有约束不回退() throws Exception {
        mockMvc.perform(bareGet("/api/v1/tenants?pageNum=1&pageSize=10")
                        .header("Authorization", auth))
                .andExpect(status().isForbidden());
    }
}
