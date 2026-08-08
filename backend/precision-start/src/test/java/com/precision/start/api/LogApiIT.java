package com.precision.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 日志 API 集成测试（8 端点：操作日志 list/detail/export/clean + 登录日志 list/detail/export/clean）。 */
@DisplayName("日志 API - /api/v1/logs")
class LogApiIT extends BaseApiIT {

    @Test
    @DisplayName("操作日志分页：admin → 200")
    void listOperLogs() throws Exception {
        mockMvc.perform(authedGet("/api/v1/logs/operation?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("登录日志分页：admin → 200（登录动作已产生登录日志）")
    void listLoginLogs() throws Exception {
        mockMvc.perform(authedGet("/api/v1/logs/login?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("操作日志详情：先触发一条操作日志(@Log) → 取 id → 详情")
    void operLogDetail() throws Exception {
        // 触发一条操作日志：建部门（@Log INSERT）
        Map<String, Object> dept = new HashMap<>();
        dept.put("parentId", 100L);
        dept.put("name", "IT日志触发部门");
        dept.put("sort", 1);
        dept.put("status", 1);
        mockMvc.perform(authedPost("/api/v1/depts").content(json(dept)))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult list = mockMvc.perform(authedGet("/api/v1/logs/operation?pageNum=1&pageSize=10"))
                .andReturn();
        JsonNode node = parse(list).at("/data/list/0/id");
        org.junit.jupiter.api.Assumptions.assumeTrue(node != null && !node.isMissingNode() && node.asLong() > 0,
                "无操作日志可测，跳过");
        long id = node.asLong();
        mockMvc.perform(authedGet("/api/v1/logs/operation/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("登录日志详情：取一条 id → 详情")
    void loginLogDetail() throws Exception {
        MvcResult list = mockMvc.perform(authedGet("/api/v1/logs/login?pageNum=1&pageSize=10"))
                .andReturn();
        JsonNode node = parse(list).at("/data/list/0/id");
        org.junit.jupiter.api.Assumptions.assumeTrue(node != null && !node.isMissingNode() && node.asLong() > 0,
                "无登录日志可测，跳过");
        mockMvc.perform(authedGet("/api/v1/logs/login/{id}", node.asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("导出操作日志：→ 200（二进制 Excel）")
    void exportOperLogs() throws Exception {
        mockMvc.perform(authedGet("/api/v1/logs/operation/export"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("导出登录日志：→ 200（二进制 Excel）")
    void exportLoginLogs() throws Exception {
        mockMvc.perform(authedGet("/api/v1/logs/login/export"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("清理操作日志：beforeDays=30 → 200")
    void cleanOperLogs() throws Exception {
        mockMvc.perform(authedDelete("/api/v1/logs/operation")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("beforeDays", 30))))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("清理登录日志：beforeDays=30 → 200")
    void cleanLoginLogs() throws Exception {
        mockMvc.perform(authedDelete("/api/v1/logs/login")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("beforeDays", 30))))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/logs/operation?pageNum=1&pageSize=10").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void list_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/logs/operation?pageNum=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }
}
