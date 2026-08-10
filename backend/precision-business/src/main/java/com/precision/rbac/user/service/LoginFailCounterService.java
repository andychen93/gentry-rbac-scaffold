package com.precision.rbac.user.service;

import com.precision.core.config.LoginSecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败计数器（基于 Redis，多节点一致）。
 *
 * <p>key: {@code login_fail:{tenantId}:{username}}。首次失败时设置 TTL = lockMinutes，
 * 计数达到 {@code maxFailCount} 即视为锁定；TTL 到期后 Redis 自动清零、账号自动解锁。</p>
 *
 * <p>不走 Caffeine 本地缓存——限流/防重用本地即可，但账号锁定需要跨节点一致，必须用 Redis。</p>
 */
@Service
public class LoginFailCounterService {

    private static final String KEY_PREFIX = "login_fail:";

    private final StringRedisTemplate redis;
    private final LoginSecurityProperties properties;

    public LoginFailCounterService(StringRedisTemplate redis, LoginSecurityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** 记录一次登录失败；首次失败时设置锁定窗口 TTL */
    public void recordFail(Long tenantId, String username) {
        String key = key(tenantId, username);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, properties.getLockMinutes(), TimeUnit.MINUTES);
        }
    }

    /** 是否已被锁定（失败次数已达上限） */
    public boolean isLocked(Long tenantId, String username) {
        return getFailCount(tenantId, username) >= properties.getMaxFailCount();
    }

    /** 当前失败次数 */
    public long getFailCount(Long tenantId, String username) {
        String v = redis.opsForValue().get(key(tenantId, username));
        if (v == null) return 0;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 登录成功后清除计数 */
    public void clear(Long tenantId, String username) {
        redis.delete(key(tenantId, username));
    }

    private String key(Long tenantId, String username) {
        return KEY_PREFIX + tenantId + ":" + username;
    }
}
