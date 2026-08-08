package com.precision.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 在线用户 API 集成测试（2 端点）：列表 / 强制下线。 */
@DisplayName("在线用户 API - /api/v1/online-users")
class OnlineApiIT extends BaseApiIT {

    @Test
    @DisplayName("在线用户列表：admin → 200 + 数组（含本测试登录会话）")
    void listOnlineUsers() throws Exception {
        mockMvc.perform(authedGet("/api/v1/online-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("强制下线：取一个 tokenId → DELETE → 200")
    void forceLogout() throws Exception {
        MvcResult r = mockMvc.perform(authedGet("/api/v1/online-users"))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        JsonNode first = parse(r).at("/data/0/tokenId");
        org.junit.jupiter.api.Assumptions.assumeTrue(first != null && !first.isMissingNode(),
                "无在线用户可下线，跳过");
        String tokenId = first.asText();
        mockMvc.perform(authedDelete("/api/v1/online-users/{tokenId}", tokenId))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void list_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/online-users").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void list_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/online-users"))
                .andExpect(status().isUnauthorized());
    }
}
