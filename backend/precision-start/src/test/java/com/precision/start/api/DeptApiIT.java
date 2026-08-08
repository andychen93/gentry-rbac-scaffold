package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部门管理 API 集成测试（5 端点）：树查询 / 详情 / 新增 / 编辑 / 删除 + 401/403/参数校验。
 * 作为其余各 Controller IT 的模板。
 */
@DisplayName("部门管理 API - /api/v1/depts")
class DeptApiIT extends BaseApiIT {

    private static final long ROOT_DEPT_ID = 100L; // 种子：总公司

    private Map<String, Object> createBody(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("parentId", ROOT_DEPT_ID);
        body.put("name", name);
        body.put("sort", 99);
        body.put("status", 1);
        return body;
    }

    @Test
    @DisplayName("树查询：admin 可见种子部门「总公司」")
    void tree_returnsSeedDept() throws Exception {
        mockMvc.perform(authedGet("/api/v1/depts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data..name", hasItem("总公司")));
    }

    @Test
    @DisplayName("详情：GET /depts/{id} 返回该部门")
    void detail_returnsDept() throws Exception {
        mockMvc.perform(authedGet("/api/v1/depts/{id}", ROOT_DEPT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("总公司"));
    }

    @Test
    @DisplayName("CRUD 流：新增 → 详情校验 → 编辑 → 删除")
    void crud_createDetailUpdateDelete() throws Exception {
        // 1. 新增
        MvcResult created = mockMvc.perform(authedPost("/api/v1/depts").content(json(createBody("IT测试部门"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long newId = parse(created).at("/data/id").asLong();

        // 2. 详情校验
        mockMvc.perform(authedGet("/api/v1/depts/{id}", newId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("IT测试部门"));

        // 3. 编辑
        Map<String, Object> update = createBody("IT测试部门改名");
        update.put("sort", 88);
        mockMvc.perform(authedPut("/api/v1/depts/{id}", newId).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedGet("/api/v1/depts/{id}", newId))
                .andExpect(jsonPath("$.data.name").value("IT测试部门改名"));

        // 4. 删除
        mockMvc.perform(authedDelete("/api/v1/depts/{id}", newId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("参数校验：新增缺 name → 200 + PARAM_ERROR(10002)（本仓业务/校验异常统一 HTTP 200+code）")
    void create_missingName_rejected() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("parentId", ROOT_DEPT_ID);
        bad.put("sort", 1); // 缺 name
        mockMvc.perform(authedPost("/api/v1/depts").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void tree_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/depts")) // 故意不带 admin 的 auth
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void tree_lowPriv_forbidden() throws Exception {
        String noPerm = forbiddenAuth();
        mockMvc.perform(get("/api/v1/depts").header("Authorization", noPerm))
                .andExpect(status().isForbidden());
    }

    // get/put/delete 静态导入已在基类；这里直接用裸 get(...) 构造不带 admin auth 的请求
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            get(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url);
    }
}
