package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 菜单管理 API 集成测试（5 端点）。 */
@DisplayName("菜单管理 API - /api/v1/menus")
class MenuApiIT extends BaseApiIT {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Map<String, Object> createBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("parentId", 0);
        b.put("name", "IT测试菜单");
        b.put("type", 1); // 目录
        b.put("sort", 99 + SEQ.incrementAndGet());
        b.put("path", "/itest" + SEQ.get());
        b.put("permission", "itest:view" + SEQ.get());
        b.put("visible", 1);
        b.put("status", 1);
        return b;
    }

    @Test
    @DisplayName("树查询：admin → 200 + 数组")
    void tree() throws Exception {
        mockMvc.perform(authedGet("/api/v1/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("CRUD 流：新增→详情→改→删")
    void crudFlow() throws Exception {
        MvcResult r = mockMvc.perform(authedPost("/api/v1/menus").content(json(createBody())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long id = parse(r).at("/data/id").asLong();

        mockMvc.perform(authedGet("/api/v1/menus/{id}", id))
                .andExpect(jsonPath("$.data.name").value("IT测试菜单"));

        mockMvc.perform(authedPut("/api/v1/menus/{id}", id)
                        .content(json(Map.of("name", "IT测试菜单改", "sort", 88, "permission", "itest:edit"))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedDelete("/api/v1/menus/{id}", id))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void tree_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/menus").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void tree_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/menus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("参数校验：新增缺菜单名 → 200 + PARAM_ERROR(10002)")
    void create_missingName() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("parentId", 0);
        bad.put("type", 1);
        bad.put("sort", 1);
        mockMvc.perform(authedPost("/api/v1/menus").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }
}
