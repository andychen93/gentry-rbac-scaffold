package com.gentry.monitor.redis.service.impl;

import com.gentry.core.common.PageResult;
import com.gentry.monitor.redis.vo.RedisKeyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RedisKeyServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Redis Key 查询服务")
class RedisKeyServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @Mock
    private RedisKeyCommands keyCommands;

    @InjectMocks
    private RedisKeyServiceImpl service;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        lenient().when(connectionFactory.getConnection()).thenReturn(connection);
        lenient().when(connection.keyCommands()).thenReturn(keyCommands);
    }

    @Test
    @DisplayName("scanKeys: 基本扫描 + 分页")
    void scanKeys_paginated() {
        List<byte[]> mockKeys = List.of(
                "k1".getBytes(StandardCharsets.UTF_8),
                "k2".getBytes(StandardCharsets.UTF_8),
                "k3".getBytes(StandardCharsets.UTF_8),
                "k4".getBytes(StandardCharsets.UTF_8),
                "k5".getBytes(StandardCharsets.UTF_8));
        // 每次 scan() 返回新的游标，避免重复调用时游标已耗尽
        when(keyCommands.scan(any(ScanOptions.class)))
                .thenAnswer(inv -> fakeCursor(mockKeys));

        when(redisTemplate.type(any())).thenReturn(DataType.STRING);
        when(redisTemplate.getExpire(any(), eq(TimeUnit.SECONDS))).thenReturn(100L);

        PageResult<RedisKeyVO> page1 = service.scanKeys("*", 1, 2);
        assertThat(page1.getTotal()).isEqualTo(5);
        assertThat(page1.getList()).hasSize(2);
        assertThat(page1.getList().get(0).getKey()).isEqualTo("k1");
        assertThat(page1.getList().get(0).getType()).isEqualTo("string");
        assertThat(page1.getList().get(0).getTtl()).isEqualTo(100L);

        PageResult<RedisKeyVO> page3 = service.scanKeys("*", 3, 2);
        assertThat(page3.getList()).hasSize(1);
        assertThat(page3.getList().get(0).getKey()).isEqualTo("k5");
    }

    @Test
    @DisplayName("scanKeys: 空 pattern 视作 '*'，pageSize>100 被限制到 100")
    void scanKeys_defaults() {
        when(keyCommands.scan(any(ScanOptions.class))).thenReturn(fakeCursor(List.of()));
        PageResult<RedisKeyVO> result = service.scanKeys("", 0, 500);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("getKeyValue: STRING 类型正常返回")
    void getKeyValue_string() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.type("k")).thenReturn(DataType.STRING);
        when(redisTemplate.getExpire("k", TimeUnit.SECONDS)).thenReturn(60L);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn("hello");

        RedisKeyVO vo = service.getKeyValue("k");

        assertThat(vo.getKey()).isEqualTo("k");
        assertThat(vo.getType()).isEqualTo("string");
        assertThat(vo.getTtl()).isEqualTo(60L);
        assertThat(vo.getValue()).isEqualTo("hello");
    }

    @Test
    @DisplayName("getKeyValue: 大 Value 被截断到 500 字符并附加长度")
    void getKeyValue_largeValue_truncated() {
        String longValue = "a".repeat(1200);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.type("big")).thenReturn(DataType.STRING);
        when(redisTemplate.getExpire("big", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get("big")).thenReturn(longValue);

        RedisKeyVO vo = service.getKeyValue("big");

        assertThat(vo.getValue()).contains("截断");
        assertThat(vo.getValue()).contains("1200");
        // 500 字符 + 提示后缀
        assertThat(vo.getValue().length()).isLessThan(longValue.length());
    }

    @Test
    @DisplayName("getKeyValue: Key 不存在时返回 type=none / ttl=-2 / value=null")
    void getKeyValue_nonExistKey() {
        when(redisTemplate.type("none")).thenReturn(DataType.NONE);

        RedisKeyVO vo = service.getKeyValue("none");

        assertThat(vo.getType()).isEqualTo("none");
        assertThat(vo.getTtl()).isEqualTo(-2L);
        assertThat(vo.getValue()).isNull();
    }

    @Test
    @DisplayName("getKeyValue: HASH 类型格式化为 JSON")
    void getKeyValue_hash() {
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("k1", "v1");
        entries.put("k2", "v2");

        when(redisTemplate.type("h")).thenReturn(DataType.HASH);
        when(redisTemplate.getExpire("h", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("h")).thenReturn(entries);

        RedisKeyVO vo = service.getKeyValue("h");

        assertThat(vo.getType()).isEqualTo("hash");
        assertThat(vo.getValue()).contains("k1").contains("v1").contains("k2").contains("v2");
    }

    @Test
    @DisplayName("getKeyValue: LIST 类型")
    void getKeyValue_list() {
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(redisTemplate.type("l")).thenReturn(DataType.LIST);
        when(redisTemplate.getExpire("l", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.range("l", 0, -1)).thenReturn(List.of("a", "b", "c"));

        RedisKeyVO vo = service.getKeyValue("l");
        assertThat(vo.getType()).isEqualTo("list");
        assertThat(vo.getValue()).contains("a").contains("b").contains("c");
    }

    @Test
    @DisplayName("getKeyValue: SET 类型")
    void getKeyValue_set() {
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.type("s")).thenReturn(DataType.SET);
        when(redisTemplate.getExpire("s", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members("s")).thenReturn(Set.of("x"));

        RedisKeyVO vo = service.getKeyValue("s");
        assertThat(vo.getType()).isEqualTo("set");
        assertThat(vo.getValue()).contains("x");
    }

    @Test
    @DisplayName("getKeyValue: ZSET 类型")
    void getKeyValue_zset() {
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(redisTemplate.type("z")).thenReturn(DataType.ZSET);
        when(redisTemplate.getExpire("z", TimeUnit.SECONDS)).thenReturn(-1L);
        when(redisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(zsetOps.range("z", 0, -1)).thenReturn(Set.of("m1", "m2"));

        RedisKeyVO vo = service.getKeyValue("z");
        assertThat(vo.getType()).isEqualTo("zset");
        assertThat(vo.getValue()).contains("m1").contains("m2");
    }

    @Test
    @DisplayName("deleteKey: 成功返回 true")
    void deleteKey_success() {
        when(redisTemplate.delete("k")).thenReturn(true);
        assertThat(service.deleteKey("k")).isTrue();
    }

    @Test
    @DisplayName("deleteKey: Key 不存在返回 false")
    void deleteKey_notExist() {
        when(redisTemplate.delete("k")).thenReturn(false);
        assertThat(service.deleteKey("k")).isFalse();
    }

    @Test
    @DisplayName("truncate: 边界场景")
    void truncate_boundaries() {
        assertThat(service.truncate(null)).isNull();
        assertThat(service.truncate("short")).isEqualTo("short");
        String exact = "a".repeat(500);
        assertThat(service.truncate(exact)).isEqualTo(exact);
        String over = "b".repeat(501);
        assertThat(service.truncate(over)).contains("截断").contains("501");
    }

    // ============ 辅助 ============

    private Cursor<byte[]> fakeCursor(List<byte[]> data) {
        List<byte[]> list = new ArrayList<>(data);
        Iterator<byte[]> it = list.iterator();
        return new Cursor<byte[]>() {
            @Override public boolean hasNext() { return it.hasNext(); }
            @Override public byte[] next() { return it.next(); }
            @Override public void close() {}
            @Override public long getCursorId() { return 0; }
            @Override public long getPosition() { return 0; }
            @Override public boolean isClosed() { return false; }
        };
    }
}
