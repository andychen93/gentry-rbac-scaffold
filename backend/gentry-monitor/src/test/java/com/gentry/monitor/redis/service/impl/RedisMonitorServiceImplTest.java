package com.gentry.monitor.redis.service.impl;

import com.gentry.monitor.redis.vo.RedisCommandStatVO;
import com.gentry.monitor.redis.vo.RedisInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

/**
 * RedisMonitorServiceImpl 单元测试 —— 聚焦解析逻辑，无需真实 Redis。
 */
@DisplayName("Redis 监控服务 - 解析逻辑")
class RedisMonitorServiceImplTest {

    private RedisMonitorServiceImpl service;

    @BeforeEach
    void setup() {
        service = new RedisMonitorServiceImpl(mock(StringRedisTemplate.class));
    }

    @Test
    @DisplayName("parseInfo: 完整字段正确解析")
    void parseInfo_normalInfo_allFieldsSet() {
        Properties info = new Properties();
        info.setProperty("redis_version", "7.2.4");
        info.setProperty("redis_mode", "standalone");
        info.setProperty("uptime_in_seconds", "86400");
        info.setProperty("connected_clients", "12");
        info.setProperty("used_memory", "1048576");
        info.setProperty("maxmemory", "1073741824");
        info.setProperty("db0", "keys=1523,expires=800,avg_ttl=1200000");
        info.setProperty("aof_enabled", "0");
        info.setProperty("rdb_last_save_time", "1700000000");
        // 扩展指标
        info.setProperty("total_commands_processed", "999999");
        info.setProperty("total_connections_received", "100");
        info.setProperty("instantaneous_ops_per_sec", "120");
        info.setProperty("keyspace_hits", "9000");
        info.setProperty("keyspace_misses", "1000");
        info.setProperty("pubsub_channels", "3");
        info.setProperty("pubsub_patterns", "1");

        RedisInfoVO vo = service.parseInfo(info);

        assertThat(vo.getRedisVersion()).isEqualTo("7.2.4");
        assertThat(vo.getRedisMode()).isEqualTo("standalone");
        assertThat(vo.getUptimeInSeconds()).isEqualTo(86400L);
        assertThat(vo.getConnectedClients()).isEqualTo(12);
        assertThat(vo.getUsedMemory()).isEqualTo(1048576L);
        assertThat(vo.getMaxMemory()).isEqualTo(1073741824L);
        assertThat(vo.getUsedMemoryPercent()).isEqualTo(0.1, within(0.01));
        assertThat(vo.getTotalKeys()).isEqualTo(1523L);
        assertThat(vo.getExpiresKeys()).isEqualTo(800L);
        assertThat(vo.getAvgTtl()).isEqualTo(1200000L);
        assertThat(vo.getAofEnabled()).isEqualTo("0");
        assertThat(vo.getRdbLastSaveTime()).isEqualTo("1700000000");

        assertThat(vo.getTotalCommandsProcessed()).isEqualTo(999999L);
        assertThat(vo.getTotalConnectionsReceived()).isEqualTo(100L);
        assertThat(vo.getInstantaneousOpsPerSec()).isEqualTo(120L);
        assertThat(vo.getKeyspaceHits()).isEqualTo(9000L);
        assertThat(vo.getKeyspaceMisses()).isEqualTo(1000L);
        // 命中率 9000/(9000+1000) = 90.0%
        assertThat(vo.getHitRate()).isEqualTo(90.0, within(0.01));
        assertThat(vo.getPubsubChannels()).isEqualTo(3);
        assertThat(vo.getPubsubPatterns()).isEqualTo(1);
    }

    @Test
    @DisplayName("parseInfo: maxmemory=0 时使用率为 0%")
    void parseInfo_zeroMaxMemory_percentZero() {
        Properties info = new Properties();
        info.setProperty("used_memory", "1048576");
        info.setProperty("maxmemory", "0");

        RedisInfoVO vo = service.parseInfo(info);
        assertThat(vo.getUsedMemoryPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("parseInfo: null / 空 Properties 不抛异常")
    void parseInfo_nullOrEmpty_noException() {
        assertThat(service.parseInfo(null).getUsedMemoryPercent()).isEqualTo(0.0);
        assertThat(service.parseInfo(new Properties()).getUsedMemoryPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("parseInfo: 非法数字字段不抛异常，返回 null")
    void parseInfo_invalidNumber_noException() {
        Properties info = new Properties();
        info.setProperty("used_memory", "abc");
        info.setProperty("maxmemory", "xyz");

        RedisInfoVO vo = service.parseInfo(info);

        assertThat(vo.getUsedMemory()).isNull();
        assertThat(vo.getMaxMemory()).isNull();
        assertThat(vo.getUsedMemoryPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("parseKeyspace: 正确解析 keys/expires/avg_ttl")
    void parseKeyspace_standard_parsed() {
        RedisInfoVO vo = new RedisInfoVO();
        service.parseKeyspace("keys=100,expires=50,avg_ttl=600", vo);

        assertThat(vo.getTotalKeys()).isEqualTo(100L);
        assertThat(vo.getExpiresKeys()).isEqualTo(50L);
        assertThat(vo.getAvgTtl()).isEqualTo(600L);
    }

    @Test
    @DisplayName("parseCommandStats: 按 calls 降序排序")
    void parseCommandStats_sortedByCallsDesc() {
        Properties stats = new Properties();
        stats.setProperty("cmdstat_set", "calls=30000,usec=90000,usec_per_call=3.00");
        stats.setProperty("cmdstat_get", "calls=50000,usec=120000,usec_per_call=2.40");
        stats.setProperty("cmdstat_del", "calls=100,usec=500,usec_per_call=5.00");

        List<RedisCommandStatVO> list = service.parseCommandStats(stats);

        assertThat(list).hasSize(3);
        assertThat(list.get(0).getName()).isEqualTo("get");
        assertThat(list.get(0).getCalls()).isEqualTo(50000L);
        assertThat(list.get(1).getName()).isEqualTo("set");
        assertThat(list.get(2).getName()).isEqualTo("del");

        assertThat(list.get(0).getUsec()).isEqualTo(120000L);
        assertThat(list.get(0).getUsecPerCall()).isEqualTo(2L); // 小数向下取整
    }

    @Test
    @DisplayName("parseCommandStats: 非 cmdstat_ 前缀的 key 忽略")
    void parseCommandStats_ignoreNonCmdstat() {
        Properties stats = new Properties();
        stats.setProperty("cmdstat_get", "calls=50,usec=100,usec_per_call=2");
        stats.setProperty("something_else", "whatever");

        List<RedisCommandStatVO> list = service.parseCommandStats(stats);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("get");
    }

    @Test
    @DisplayName("parseCommandStats: 空 Properties 返回空列表")
    void parseCommandStats_empty_returnsEmpty() {
        assertThat(service.parseCommandStats(null)).isEmpty();
        assertThat(service.parseCommandStats(new Properties())).isEmpty();
    }

    @Test
    @DisplayName("parseInfo: 命中率在 hits+misses=0 时为 0")
    void parseInfo_hitRate_zeroDenominator() {
        Properties info = new Properties();
        info.setProperty("keyspace_hits", "0");
        info.setProperty("keyspace_misses", "0");
        RedisInfoVO vo = service.parseInfo(info);
        assertThat(vo.getHitRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getKeyDefines: 返回预定义 Key 列表")
    void getKeyDefines_returnsDefines() {
        assertThat(service.getKeyDefines()).isNotEmpty();
        assertThat(service.getKeyDefines())
                .extracting("keyType")
                .contains("token_mapping", "user_session", "jwt_blacklist");
    }

    @Test
    @DisplayName("parseSlowLog: 空输入返回空列表")
    void parseSlowLog_empty() {
        assertThat(service.parseSlowLog(null)).isEmpty();
        assertThat(service.parseSlowLog(java.util.Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("parseSlowLog: 标准 6 字段解析，按耗时降序")
    void parseSlowLog_sixFields_sortedByDuration() {
        List<Object> row1 = List.of(
                1L, 1700000000L, 500L,
                List.of("GET", "foo"),
                "127.0.0.1:5555", "cli-a");
        List<Object> row2 = List.of(
                2L, 1700000001L, 8000L,
                List.of("KEYS", "*"),
                "127.0.0.1:6666", "cli-b");
        List<Object> raw = List.of(row1, row2);

        var logs = service.parseSlowLog(raw);

        assertThat(logs).hasSize(2);
        // 8000us 的排第一
        assertThat(logs.get(0).getId()).isEqualTo(2L);
        assertThat(logs.get(0).getDurationMicros()).isEqualTo(8000L);
        assertThat(logs.get(0).getArgs()).containsExactly("KEYS", "*");
        assertThat(logs.get(0).getClientAddress()).isEqualTo("127.0.0.1:6666");
        assertThat(logs.get(0).getClientName()).isEqualTo("cli-b");

        assertThat(logs.get(1).getId()).isEqualTo(1L);
        assertThat(logs.get(1).getArgs()).containsExactly("GET", "foo");
    }

    @Test
    @DisplayName("parseSlowLog: 老版本 4 字段（无 clientAddr/Name）也能解析")
    void parseSlowLog_fourFields() {
        List<Object> row = List.of(
                1L, 1700000000L, 100L,
                List.of("DEL", "x"));
        var logs = service.parseSlowLog(List.of(row));
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getClientAddress()).isNull();
        assertThat(logs.get(0).getClientName()).isNull();
    }

    @Test
    @DisplayName("parseSlowLog: byte[] 参数自动按 UTF-8 解码")
    void parseSlowLog_byteArgs() {
        List<Object> row = List.of(
                1L, 1700000000L, 200L,
                List.of("SET".getBytes(), "name".getBytes(), "张三".getBytes()));
        var logs = service.parseSlowLog(List.of(row));
        assertThat(logs.get(0).getArgs()).containsExactly("SET", "name", "张三");
    }
}
