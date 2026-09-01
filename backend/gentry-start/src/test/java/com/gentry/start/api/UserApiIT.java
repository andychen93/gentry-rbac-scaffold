package com.gentry.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 用户管理 API 集成测试（8 端点）。 */
@DisplayName("用户管理 API - /api/v1/users")
class UserApiIT extends BaseApiIT {

    /** 直连库造/验脏数据用（测试带 @Transactional，用完自动回滚） */
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final long ADMIN_USER_ID = 2L;
    private static final long ADMIN_ROLE_ID = 1L;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private Map<String, Object> createBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("username", "usrit" + SEQ.incrementAndGet());
        b.put("nickname", "IT用户");
        b.put("password", "Usr@123456");
        b.put("deptId", 100);
        b.put("status", 1);
        return b;
    }

    @Test
    @DisplayName("分页列表：admin → 200 + 非空")
    void list() throws Exception {
        mockMvc.perform(authedGet("/api/v1/users?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("详情：GET /users/2 → admin")
    void detail() throws Exception {
        mockMvc.perform(authedGet("/api/v1/users/{id}", ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(ADMIN));
    }

    @Test
    @DisplayName("CRUD 流：新增→改→分配角色→重置密码→切换状态→删除")
    void crudFlow() throws Exception {
        MvcResult r = mockMvc.perform(authedPost("/api/v1/users").content(json(createBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long id = parse(r).at("/data/id").asLong();

        mockMvc.perform(authedPut("/api/v1/users/{id}", id)
                        .content(json(Map.of("nickname", "IT用户改", "deptId", 100, "status", 1))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedPut("/api/v1/users/{id}/roles", id)
                        .content(json(Map.of("roleIds", java.util.List.of(ADMIN_ROLE_ID)))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedPut("/api/v1/users/{id}/password/reset", id)
                        .content(json(Map.of("newPassword", "New@123456"))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedPut("/api/v1/users/{id}/status", id).content(json(Map.of("status", 0))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedPut("/api/v1/users/{id}/status", id).content(json(Map.of("status", 1))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedDelete("/api/v1/users/{id}", id))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("用户选项：GET /users/options → 200 + 数组")
    void options() throws Exception {
        mockMvc.perform(authedGet("/api/v1/users/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].username").exists());
    }

    @Test
    @DisplayName("详情：roleIds 过滤掉指向已删除/不存在角色的脏关联")
    void detail_filtersOrphanRoleIds() throws Exception {
        /*
         * 回归：sys_user_role 里若残留指向不存在角色的行（历史上「编辑用户」不校验
         * roleIds 就会写进来），detail 直读关联表会把它返回给前端；
         * 「分配角色」弹窗以此初始化选中项，而候选列表不含该角色 →
         * 这个 id 界面上看不见却会被提交，报「角色不存在: [xxx]」且用户无法自救。
         */
        long orphanRoleId = 999999999999L;
        long orphanRowId = 987654321L;
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_user_role (id, user_id, role_id, create_time) "
                            + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    orphanRowId, ADMIN_USER_ID, orphanRoleId);

            // 脏行确实在库里
            Integer raw = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = ?",
                    Integer.class, ADMIN_USER_ID, orphanRoleId);
            org.assertj.core.api.Assertions.assertThat(raw).isEqualTo(1);

            // 但接口不该把它吐出来
            mockMvc.perform(authedGet("/api/v1/users/{id}", ADMIN_USER_ID))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.roleIds").isArray())
                    .andExpect(jsonPath("$.data.roleIds[?(@ == '" + orphanRoleId + "')]").doesNotExist());
        } finally {
            // 显式清理：不能只依赖 @Transactional 回滚 —— 实测这条脏行会残留到库里，
            // 留下去会让「孤儿关联」检查永远非零，掩盖真实问题
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE id = ?", orphanRowId);
        }
    }

    @Test
    @DisplayName("编辑用户：roleIds 含不存在角色 → 200 + ROLE_NOT_FOUND(20015)，且不落脏数据")
    void update_rejectsUnknownRoleIds() throws Exception {
        // create/assignRoles 一直有校验，update 漏了 —— 脏关联就是从这个口子进来的
        Map<String, Object> body = new HashMap<>();
        body.put("nickname", "IT用户改");
        body.put("deptId", 100);
        body.put("status", 1);
        body.put("roleIds", java.util.List.of(999999999999L));

        mockMvc.perform(authedPut("/api/v1/users/{id}", ADMIN_USER_ID).content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20015));

        Integer polluted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = ?",
                Integer.class, ADMIN_USER_ID, 999999999999L);
        org.assertj.core.api.Assertions.assertThat(polluted).isZero();
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        // 用 forbiddenAuth()（无角色用户）稳定触发 403。
        String noPerm = forbiddenAuth();
        mockMvc.perform(bareGet("/api/v1/users?pageNum=1&pageSize=10").header("Authorization", noPerm))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void list_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/users?pageNum=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("参数校验：新增缺用户名 → 200 + PARAM_ERROR(10002)")
    void create_missingUsername() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("nickname", "无用户名");
        bad.put("password", "Usr@123456");
        bad.put("deptId", 100);
        mockMvc.perform(authedPost("/api/v1/users").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("导出模板：GET /users/import/template → 200 + xlsx")
    void importTemplate() throws Exception {
        mockMvc.perform(authedGet("/api/v1/users/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("user_import_template.xlsx")));
    }

    @Test
    @DisplayName("导出：GET /users/export → 200 + xlsx")
    void exportUsers() throws Exception {
        mockMvc.perform(authedGet("/api/v1/users/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("users.xlsx")));
    }

    @Test
    @DisplayName("导入：POST /users/import 上传空模板 → 200 + success=0")
    void importUsers_emptyTemplate() throws Exception {
        byte[] template = mockMvc.perform(authedGet("/api/v1/users/import/template"))
                .andReturn().getResponse().getContentAsByteArray();
        mockMvc.perform(multipart("/api/v1/users/import").file("file", template).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(0));
    }
}
