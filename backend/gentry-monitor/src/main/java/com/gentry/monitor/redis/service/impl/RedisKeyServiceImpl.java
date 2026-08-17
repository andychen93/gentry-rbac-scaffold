package com.gentry.monitor.redis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentry.core.common.PageResult;
import com.gentry.monitor.redis.service.RedisKeyService;
import com.gentry.monitor.redis.vo.RedisKeyVO;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Key 查询与操作服务实现。
 *
 * <p>扫描使用 {@code SCAN} 游标避免阻塞；分页在内存中完成（Redis SCAN 不支持服务端分页）。
 * 为防止误操作或内存耗尽，单次 SCAN 最多收集 {@link #MAX_SCAN_KEYS} 个 Key。</p>
 */
@Service
public class RedisKeyServiceImpl implements RedisKeyService {

    /** 单次扫描最多收集的 Key 数，防止 pattern 过于宽泛导致 OOM */
    static final int MAX_SCAN_KEYS = 10_000;

    /** Value 显示截断长度 */
    static final int MAX_VALUE_LENGTH = 500;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisKeyServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public PageResult<RedisKeyVO> scanKeys(String pattern, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;
        if (pattern == null || pattern.isBlank()) pattern = "*";

        Set<String> keys = scanAllKeys(pattern);

        List<String> keyList = new ArrayList<>(keys);
        int fromIndex = Math.min((pageNum - 1) * pageSize, keyList.size());
        int toIndex = Math.min(fromIndex + pageSize, keyList.size());
        List<String> pageKeys = keyList.subList(fromIndex, toIndex);

        List<RedisKeyVO> voList = new ArrayList<>(pageKeys.size());
        for (String k : pageKeys) {
            RedisKeyVO vo = new RedisKeyVO();
            vo.setKey(k);
            DataType type = redisTemplate.type(k);
            vo.setType(type == null ? DataType.NONE.code() : type.code());
            vo.setTtl(redisTemplate.getExpire(k, TimeUnit.SECONDS));
            voList.add(vo);
        }
        return new PageResult<>(voList, keyList.size(), pageNum, pageSize);
    }

    @Override
    public RedisKeyVO getKeyValue(String key) {
        RedisKeyVO vo = new RedisKeyVO();
        vo.setKey(key);
        DataType dataType = redisTemplate.type(key);
        if (dataType == null || dataType == DataType.NONE) {
            vo.setType(DataType.NONE.code());
            vo.setTtl(-2L);
            vo.setValue(null);
            return vo;
        }
        vo.setType(dataType.code());
        vo.setTtl(redisTemplate.getExpire(key, TimeUnit.SECONDS));
        vo.setValue(truncate(readValueByType(key, dataType)));
        return vo;
    }

    @Override
    public boolean deleteKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // ================ internal ================

    private Set<String> scanAllKeys(String pattern) {
        Set<String> keys = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        RedisConnectionFactory factory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (RedisConnection connection = factory.getConnection();
             Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext() && keys.size() < MAX_SCAN_KEYS) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
        return keys;
    }

    private String readValueByType(String key, DataType dataType) {
        return switch (dataType) {
            case STRING -> redisTemplate.opsForValue().get(key);
            case HASH -> {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                yield toJson(entries);
            }
            case LIST -> toJson(redisTemplate.opsForList().range(key, 0, -1));
            case SET -> toJson(redisTemplate.opsForSet().members(key));
            case ZSET -> toJson(redisTemplate.opsForZSet().range(key, 0, -1));
            default -> "unsupported type: " + dataType.code();
        };
    }

    String truncate(String value) {
        if (value == null) return null;
        if (value.length() <= MAX_VALUE_LENGTH) return value;
        return value.substring(0, MAX_VALUE_LENGTH) + "...(截断，总长度:" + value.length() + ")";
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
}
