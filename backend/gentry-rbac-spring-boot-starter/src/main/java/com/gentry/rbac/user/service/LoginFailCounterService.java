package com.gentry.rbac.user.service;

import com.gentry.core.config.LoginSecurityProperties;
import com.gentry.rbac.config.SysConfigResolver;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败计数器（基于 Redis，多节点一致）。
 *
 * <p>key: {@code login_fail:{username}}。首次失败时设置 TTL = lockMinutes，
 * 计数达到 {@code maxFailCount} 即视为锁定；TTL 到期后 Redis 自动清零、账号自动解锁。</p>
 *
 * <p>不走 Caffeine 本地缓存——限流/防重用本地即可，但账号锁定需要跨节点一致，必须用 Redis。</p>
 *
 * <p>阈值与锁定时长优先取 sys_config（参数配置页可改，改完即时生效），
 * 库里没配才用 yml 的 {@link LoginSecurityProperties} 兜底。</p>
 */
@Service
public class LoginFailCounterService {

    private static final String KEY_PREFIX = "login_fail:";

    private final StringRedisTemplate redis;
    private final LoginSecurityProperties properties;
    private final SysConfigResolver sysConfig;

    public LoginFailCounterService(StringRedisTemplate redis,
                                   LoginSecurityProperties properties,
                                   SysConfigResolver sysConfig) {
        this.redis = redis;
        this.properties = properties;
        this.sysConfig = sysConfig;
    }

    /** 锁定时长（分钟）：sys_config 优先，yml 兜底 */
    public int lockMinutes() {
        return sysConfig.getInt(SysConfigResolver.KEY_LOGIN_LOCK_MINUTES, properties.getLockMinutes());
    }

    /** 连续失败上限：sys_config 优先，yml 兜底 */
    public int maxFailCount() {
        return sysConfig.getInt(SysConfigResolver.KEY_LOGIN_MAX_FAIL_COUNT, properties.getMaxFailCount());
    }

    /** 记录一次登录失败；首次失败时设置锁定窗口 TTL */
    public void recordFail(String username) {
        String key = key(username);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, lockMinutes(), TimeUnit.MINUTES);
        }
    }

    /** 是否已被锁定（失败次数已达上限） */
    public boolean isLocked(String username) {
        return getFailCount(username) >= maxFailCount();
    }

    /** 当前失败次数 */
    public long getFailCount(String username) {
        String v = redis.opsForValue().get(key(username));
        if (v == null) return 0;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 登录成功后清除计数 */
    public void clear(String username) {
        redis.delete(key(username));
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
