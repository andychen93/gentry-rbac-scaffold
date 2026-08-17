package com.gentry.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 角色管理 API 集成测试（9 端点）。 */
@DisplayName("角色管理 API - /api/v1/roles")
class RoleApiIT extends BaseApiIT {

    private static final long ADMIN_ROLE_ID = 1L;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private Map<String, Object> createBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("roleCode", "roleit" + SEQ.incrementAndGet());
        b.put("roleName", "IT测试角色");
        b.put("dataScope", 1);
        b.put("sort", 99);
        b.put("status", 1);
        return b;
    }

    @Test
    @DisplayName("分页列表：admin → 200 + 非空")
    void list() throws Exception {
        mockMvc.perform(authedGet("/api/v1/roles?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("详情：GET /roles/1")
    void detail() throws Exception {
        mockMvc.perform(authedGet("/api/v1/roles/{id}", ADMIN_ROLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"));
    }

    @Test
    @DisplayName("完整流：新增→改→分配菜单→数据权限→状态→查用户→删除")
    void fullFlow() throws Exception {
        MvcResult r = mockMvc.perform(authedPost("/api/v1/roles").content(json(createBody())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long id = parse(r).at("/data/id").asLong();

        mockMvc.perform(authedPut("/api/v1/roles/{id}", id)
                        .content(json(Map.of("roleName", "IT测试角色改", "sort", 88))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedPut("/api/v1/roles/{id}/menus", id).content(json(Map.of("menuIds", List.of()))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedPut("/api/v1/roles/{id}/data-scope", id)
                        .content(json(Map.of("dataScope", 1))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedPut("/api/v1/roles/{id}/status", id).content(json(Map.of("status", 0))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedPut("/api/v1/roles/{id}/status", id).content(json(Map.of("status", 1))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedGet("/api/v1/roles/{id}/users", id))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedDelete("/api/v1/roles/{id}", id))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/roles?pageNum=1&pageSize=10").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void list_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/roles?pageNum=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("参数校验：新增缺角色名称 → 200 + PARAM_ERROR(10002)")
    void create_missingRoleName() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("roleCode", "roleitbad" + SEQ.incrementAndGet());
        bad.put("sort", 1);
        mockMvc.perform(authedPost("/api/v1/roles").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("绑定用户：绑定→查回→全量覆盖→解绑全部")
    void assignUsers_fullFlow() throws Exception {
        MvcResult r = mockMvc.perform(authedPost("/api/v1/roles").content(json(createBody())))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long roleId = parse(r).at("/data/id").asLong();

        // 绑定 admin(2) + zhangsan(3)
        mockMvc.perform(authedPut("/api/v1/roles/{id}/users", roleId)
                        .content(json(Map.of("userIds", List.of(2, 3)))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedGet("/api/v1/roles/{id}/users", roleId))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2));

        // 全量覆盖：只留 zhangsan
        mockMvc.perform(authedPut("/api/v1/roles/{id}/users", roleId)
                        .content(json(Map.of("userIds", List.of(3)))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedGet("/api/v1/roles/{id}/users", roleId))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0]").value("3"));

        // 空数组 = 解绑全部
        mockMvc.perform(authedPut("/api/v1/roles/{id}/users", roleId)
                        .content(json(Map.of("userIds", List.of()))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedGet("/api/v1/roles/{id}/users", roleId))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("绑定用户：userId 不存在 → 200 + USER_NOT_FOUND(20013)")
    void assignUsers_unknownUser() throws Exception {
        mockMvc.perform(authedPut("/api/v1/roles/{id}/users", ADMIN_ROLE_ID)
                        .content(json(Map.of("userIds", List.of(999999999999L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20013));
    }

    @Test
    @DisplayName("绑定用户：userIds 重复 → 去重，不触发唯一约束")
    void assignUsers_duplicateIds() throws Exception {
        MvcResult r = mockMvc.perform(authedPost("/api/v1/roles").content(json(createBody())))
                .andReturn();
        long roleId = parse(r).at("/data/id").asLong();

        mockMvc.perform(authedPut("/api/v1/roles/{id}/users", roleId)
                        .content(json(Map.of("userIds", List.of(3, 3, 3)))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedGet("/api/v1/roles/{id}/users", roleId))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("绑定用户：userIds 为 null → 200 + PARAM_ERROR(10002)")
    void assignUsers_nullUserIds() throws Exception {
        mockMvc.perform(authedPut("/api/v1/roles/{id}/users", ADMIN_ROLE_ID)
                        .content(json(new HashMap<String, Object>())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("下拉选项：GET /roles/options → 200 + 启用角色数组")
    void options() throws Exception {
        mockMvc.perform(authedGet("/api/v1/roles/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
