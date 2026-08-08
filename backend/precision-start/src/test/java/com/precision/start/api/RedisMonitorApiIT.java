package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Redis 监控 API 集成测试（7 端点）。 */
@DisplayName("Redis 监控 API - /api/v1/monitor/redis")
class RedisMonitorApiIT extends BaseApiIT {

    @Autowired
    private StringRedisTemplate redis;

    @Test
    @DisplayName("监控信息：GET /info → 200")
    void info() throws Exception {
        mockMvc.perform(authedGet("/api/v1/monitor/redis/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Key 定义列表：GET /key-defines → 200")
    void keyDefines() throws Exception {
        mockMvc.perform(authedGet("/api/v1/monitor/redis/key-defines"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Key 扫描：GET /keys?pattern=* → 200")
    void scanKeys() throws Exception {
        mockMvc.perform(authedGet("/api/v1/monitor/redis/keys?pattern=*&pageNum=1&pageSize=10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("查 Key 值：写探针 key → GET /keys/{key}/value → 200")
    void keyValue() throws Exception {
        redis.opsForValue().set("itest:probe:value", "hello");
        mockMvc.perform(authedGet("/api/v1/monitor/redis/keys/{key}/value", "itest:probe:value"))
                .andExpect(jsonPath("$.code").value(0));
        redis.delete("itest:probe:value");
    }

    @Test
    @DisplayName("删 Key：写探针 key → DELETE /keys/{key} → 200")
    void deleteKey() throws Exception {
        redis.opsForValue().set("itest:probe:delete", "bye");
        mockMvc.perform(authedDelete("/api/v1/monitor/redis/keys/{key}", "itest:probe:delete"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("慢日志：GET /slowlog → 200")
    void slowlog() throws Exception {
        mockMvc.perform(authedGet("/api/v1/monitor/redis/slowlog?limit=10"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("重置慢日志：DELETE /slowlog → 200")
    void resetSlowlog() throws Exception {
        mockMvc.perform(authedDelete("/api/v1/monitor/redis/slowlog"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void info_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/monitor/redis/info").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void info_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/monitor/redis/info"))
                .andExpect(status().isUnauthorized());
    }
}
