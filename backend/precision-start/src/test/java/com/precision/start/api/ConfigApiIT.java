package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 参数配置 API 集成测试（6 端点：分页/增/改/删/刷新缓存）。 */
@DisplayName("参数配置 API - /api/v1/configs")
class ConfigApiIT extends BaseApiIT {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Map<String, Object> createBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("configName", "IT参数");
        b.put("configKey", "it.config.key" + SEQ.incrementAndGet());
        b.put("configValue", "v1");
        b.put("configType", "N");
        b.put("remark", "测试");
        return b;
    }

    @Test
    @DisplayName("分页列表：admin → 200，含 V8 预置参数")
    void list() throws Exception {
        mockMvc.perform(authedGet("/api/v1/configs?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("完整流：新增→改→刷新缓存→查询→删除")
    void fullFlow() throws Exception {
        MvcResult r = mockMvc.perform(authedPost("/api/v1/configs").content(json(createBody())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long id = parse(r).at("/data/id").asLong();

        mockMvc.perform(authedPut("/api/v1/configs/{id}", id)
                        .content(json(Map.of("configValue", "v2", "remark", "改后"))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedDelete("/api/v1/configs/cache"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedGet("/api/v1/configs?pageNum=1&pageSize=10"))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(authedDelete("/api/v1/configs/{id}", id))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("参数键唯一：重复 key → 200 + PARAM_ERROR(10002)")
    void create_duplicateKey() throws Exception {
        Map<String, Object> b = createBody();
        mockMvc.perform(authedPost("/api/v1/configs").content(json(b)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(authedPost("/api/v1/configs").content(json(b)))
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("参数校验：新增缺 configKey → 200 + PARAM_ERROR(10002)")
    void create_missingKey() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("configName", "无键");
        mockMvc.perform(authedPost("/api/v1/configs").content(json(bad)))
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/configs?pageNum=1&pageSize=10").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void list_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/configs?pageNum=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }
}
