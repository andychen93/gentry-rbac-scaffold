package com.precision.start.api;

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

/** 用户管理 API 集成测试（8 端点）。chenli 缺 system:user:* → 用 chenli 演示 403。 */
@DisplayName("用户管理 API - /api/v1/users")
class UserApiIT extends BaseApiIT {

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
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        // 用 forbiddenAuth()（无角色用户）稳定触发 403。
        // 不用 chenli：chenli 兼具 SUPER_ADMIN，Phase 6 后作为平台超管跨租户，
        // getTenantIds 返回 null 跳过租户过滤，真正拥有全部权限（含 system:user:list）。
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
