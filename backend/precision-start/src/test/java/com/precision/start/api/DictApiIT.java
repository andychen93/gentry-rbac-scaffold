package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 字典管理 API 集成测试（9 端点：字典类型 + 字典数据 + 缓存刷新）。 */
@DisplayName("字典管理 API - /api/v1/dict")
class DictApiIT extends BaseApiIT {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private String newDictType() {
        return "itdict" + SEQ.incrementAndGet();
    }

    @Test
    @DisplayName("字典类型分页：admin → 200")
    void listTypes() throws Exception {
        mockMvc.perform(authedGet("/api/v1/dict/types?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("完整流：建类型→改→建数据→列表→改→删数据→删类型→刷新缓存")
    void fullFlow() throws Exception {
        String dictType = newDictType();

        // 建类型
        Map<String, Object> typeBody = new HashMap<>();
        typeBody.put("dictName", "IT字典");
        typeBody.put("dictType", dictType);
        typeBody.put("status", 1);
        MvcResult tr = mockMvc.perform(authedPost("/api/v1/dict/types").content(json(typeBody)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long typeId = parse(tr).at("/data/id").asLong();

        // 改类型
        mockMvc.perform(authedPut("/api/v1/dict/types/{id}", typeId).content(json(Map.of("dictName", "IT字典改"))))
                .andExpect(jsonPath("$.code").value(0));

        // 字典数据列表（空）
        mockMvc.perform(authedGet("/api/v1/dict/types/{dictType}/data", dictType))
                .andExpect(jsonPath("$.code").value(0));

        // 建数据
        Map<String, Object> dataBody = new HashMap<>();
        dataBody.put("dictLabel", "是");
        dataBody.put("dictValue", "1");
        dataBody.put("sort", 1);
        dataBody.put("status", 1);
        MvcResult dr = mockMvc.perform(authedPost("/api/v1/dict/types/" + dictType + "/data").content(json(dataBody)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long dataId = parse(dr).at("/data/id").asLong();

        // 改数据
        mockMvc.perform(authedPut("/api/v1/dict/data/{id}", dataId)
                        .content(json(Map.of("dictLabel", "否", "dictValue", "0"))))
                .andExpect(jsonPath("$.code").value(0));

        // 删数据
        mockMvc.perform(authedDelete("/api/v1/dict/data/{id}", dataId))
                .andExpect(jsonPath("$.code").value(0));

        // 删类型
        mockMvc.perform(authedDelete("/api/v1/dict/types/{id}", typeId))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("刷新缓存：DELETE /dict/cache → 200")
    void refreshCache() throws Exception {
        mockMvc.perform(authedDelete("/api/v1/dict/cache"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void listTypes_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/dict/types?pageNum=1&pageSize=10").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void listTypes_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/dict/types?pageNum=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("参数校验：建类型缺名称 → 200 + PARAM_ERROR(10002)")
    void createType_missingName() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("dictType", newDictType());
        mockMvc.perform(authedPost("/api/v1/dict/types").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }
}
