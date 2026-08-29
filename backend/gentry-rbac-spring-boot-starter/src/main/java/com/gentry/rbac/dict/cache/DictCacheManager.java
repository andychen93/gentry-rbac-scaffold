package com.gentry.rbac.dict.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gentry.rbac.dict.vo.DictDataVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 字典二级缓存：<b>L1 Caffeine（本地） + L2 Redis（跨节点共享）</b>。
 *
 * <pre>
 * 读：L1 命中 → 返回
 *     L1 未命中 → 查 L2；命中则回填 L1 并返回
 *     都未命中 → 交由调用方查库，再 put(...) 同时写入 L1+L2
 * 写/删：删 L2 → 广播失效消息 → 各节点（含自己）清掉本地 L1
 * </pre>
 *
 * <p>为什么必须有 L2 和广播：原实现只有一个 Caffeine，是<b>单 JVM</b> 的。
 * 多节点部署时 A 节点改字典只能清掉自己的缓存，B 节点会继续拿旧值直到
 * 本地 TTL 到期；「刷新缓存」按钮也只会打到负载均衡选中的那一个节点。
 * 现在权威副本放在 Redis，本地 L1 只是短 TTL 的读加速，且失效通过
 * pub/sub 广播到所有节点，跨节点一致。</p>
 *
 * <p>L1 TTL 故意远短于 L2：L1 只为削峰，即使广播消息丢失（Redis pub/sub
 * 不保证投递），脏数据最多存活 {@value #L1_TTL_MINUTES} 分钟而不是 30 分钟。</p>
 */
@Component
public class DictCacheManager {

    private static final Logger log = LoggerFactory.getLogger(DictCacheManager.class);

    /** Redis Key 前缀，登记在 RedisKeyDefines，可在 Redis 监控页查看 */
    public static final String KEY_PREFIX = "dict:";
    /** 失效广播频道 */
    public static final String INVALIDATE_CHANNEL = "dict:invalidate";
    /** 广播「全部失效」时的载荷 */
    public static final String PAYLOAD_ALL = "*";

    private static final long L1_TTL_MINUTES = 2;
    private static final long L2_TTL_MINUTES = 30;
    private static final TypeReference<List<DictDataVO>> LIST_TYPE = new TypeReference<>() {};

    private final Cache<String, List<DictDataVO>> l1 = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(L1_TTL_MINUTES, TimeUnit.MINUTES)
            .build();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public DictCacheManager(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 读缓存；两级都未命中返回 null（由调用方查库） */
    public List<DictDataVO> get(Long tenantId, String dictType) {
        String key = key(tenantId, dictType);
        List<DictDataVO> local = l1.getIfPresent(key);
        if (local != null) {
            return local;
        }
        try {
            String json = redis.opsForValue().get(key);
            if (json != null) {
                List<DictDataVO> fromRedis = objectMapper.readValue(json, LIST_TYPE);
                l1.put(key, fromRedis);   // 回填本地
                return fromRedis;
            }
        } catch (Exception e) {
            // Redis 不可用或反序列化失败时降级为「未命中」，让调用方查库，不影响功能
            log.warn("Dict L2 read failed, falling back to DB: key={}, {}", key, e.getMessage());
        }
        return null;
    }

    /** 同时写入 L1 与 L2 */
    public void put(Long tenantId, String dictType, List<DictDataVO> data) {
        String key = key(tenantId, dictType);
        l1.put(key, data);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(data), L2_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Dict L2 write failed (local cache only): key={}, {}", key, e.getMessage());
        }
    }

    /** 失效单个字典类型：删 L2 + 广播，各节点据此清本地 */
    public void invalidate(Long tenantId, String dictType) {
        String key = key(tenantId, dictType);
        l1.invalidate(key);
        try {
            redis.delete(key);
            redis.convertAndSend(INVALIDATE_CHANNEL, key);
        } catch (Exception e) {
            log.warn("Dict cache invalidation broadcast failed (local already cleared): key={}, {}", key, e.getMessage());
        }
    }

    /** 失效全部：清 L1 + 删所有 dict:* + 广播 */
    public void invalidateAll() {
        l1.invalidateAll();
        try {
            Set<String> keys = redis.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
            redis.convertAndSend(INVALIDATE_CHANNEL, PAYLOAD_ALL);
        } catch (Exception e) {
            log.warn("Dict cache full invalidation broadcast failed (local already cleared): {}", e.getMessage());
        }
        log.info("Dict cache refreshed (L1+L2)");
    }

    /**
     * 收到其它节点的失效广播时调用：只清本地 L1。
     * L2 由发起方删除，这里不必重复删。
     */
    public void onInvalidateMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        if (PAYLOAD_ALL.equals(payload)) {
            l1.invalidateAll();
        } else {
            l1.invalidate(payload);
        }
        if (log.isDebugEnabled()) {
            log.debug("Received dict cache invalidation broadcast: {}", payload);
        }
    }

    /** 本地缓存条目数（测试/排查用） */
    public long localSize() {
        return l1.estimatedSize();
    }

    private String key(Long tenantId, String dictType) {
        return KEY_PREFIX + tenantId + ":" + dictType;
    }
}
