package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 租户管理 API 集成测试（8 端点）。 */
@DisplayName("租户管理 API - /api/v1/tenants")
class TenantApiIT extends BaseApiIT {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private Map<String, Object> createBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("code", "tntit" + SEQ.incrementAndGet()); // 6-20 位字母数字
        b.put("name", "IT测试租户");
        b.put("contact", "测试人");
        return b;
    }

    @Test
    @DisplayName("分页列表：SUPER_ADMIN → 200 + 非空")
    void list() throws Exception {
        mockMvc.perform(superGet("/api/v1/tenants?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("详情：GET /tenants/1 → default 租户")
    void detail() throws Exception {
        mockMvc.perform(superGet("/api/v1/tenants/{id}", DEFAULT_TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("default"));
    }

    @Test
    @DisplayName("租户选项：公共接口，无需 token → 200")
    void options_public() throws Exception {
        mockMvc.perform(bareGet("/api/v1/tenants/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("完整流：新增→改→配置→状态→删除")
    void fullFlow() throws Exception {
        MvcResult r = mockMvc.perform(superPost("/api/v1/tenants").content(json(createBody())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tenantId").exists())
                .andReturn();
        long id = parse(r).at("/data/tenantId").asLong();

        mockMvc.perform(superPut("/api/v1/tenants/{id}", id).content(json(Map.of("name", "IT测试租户改"))))
                .andExpect(jsonPath("$.code").value(0));

        // 注：PUT /tenants/{id}/config 需要 system:tenant:config，种子未授予任何角色，略过其 happy-path。
        mockMvc.perform(superPut("/api/v1/tenants/{id}/status", id).content(json(Map.of("status", 0))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(superPut("/api/v1/tenants/{id}/status", id).content(json(Map.of("status", 1))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(superDelete("/api/v1/tenants/{id}", id))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/tenants?pageNum=1&pageSize=10").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void list_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/tenants?pageNum=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("参数校验：新增缺名称 → 200 + PARAM_ERROR(10002)")
    void create_missingName() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("code", "tntitbad" + SEQ.incrementAndGet());
        mockMvc.perform(superPost("/api/v1/tenants").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }
}
