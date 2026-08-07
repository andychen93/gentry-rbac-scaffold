package com.precision.core.web;

import cn.dev33.satoken.stp.StpUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.precision.core.common.ErrorCode;
import com.precision.core.exception.BizException;
import com.precision.core.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口限流切面。
 *
 * <p>基于 Caffeine 本地缓存 + 滑动窗口（窗口大小 = period，自过期）实现：</p>
 * <ul>
 *     <li>每个 (keyType, keyValue, methodSignature) 唯一一个计数器</li>
 *     <li>计数器超过 count 抛出 {@link BizException}（{@link ErrorCode#TOO_MANY_REQUESTS}）</li>
 *     <li>Caffeine {@link Expiry} 按 key 自定义 TTL，过期自动清理</li>
 * </ul>
 *
 * <p>容量上限 10,000 个 key，LRU 驱逐，满足单机百万级 QPS 下的内存控制。</p>
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /** 缓存容量上限 */
    private static final long MAX_SIZE = 10_000L;

    /** key 前缀 */
    private static final String KEY_PREFIX = "rate_limit:";

    private final Cache<String, CounterEntry> counters;

    public RateLimitAspect() {
        this.counters = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfter(new Expiry<String, CounterEntry>() {
                    @Override
                    public long expireAfterCreate(String key, CounterEntry value, long currentTime) {
                        return TimeUnit.SECONDS.toNanos(value.periodSeconds);
                    }

                    @Override
                    public long expireAfterUpdate(String key, CounterEntry value, long currentTime,
                                                   long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, CounterEntry value, long currentTime,
                                                 long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        if (rateLimit.count() <= 0 || rateLimit.period() <= 0) {
            return pjp.proceed();
        }

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String cacheKey = buildKey(rateLimit, signature);

        CounterEntry entry = counters.get(cacheKey, k -> new CounterEntry(rateLimit.period()));
        long current = entry.counter.incrementAndGet();

        if (current > rateLimit.count()) {
            String msg = rateLimit.message() == null || rateLimit.message().isEmpty()
                    ? ErrorCode.TOO_MANY_REQUESTS.getMessage()
                    : rateLimit.message();
            log.warn("限流触发: key={}, count={}, limit={}", cacheKey, current, rateLimit.count());
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, msg);
        }
        return pjp.proceed();
    }

    /** 构建缓存 key： rate_limit:{keyType}:{keyValue}[:{key}]:{className#method} */
    String buildKey(RateLimit rateLimit, MethodSignature signature) {
        String keyValue = resolveKeyValue(rateLimit.keyType());
        StringBuilder sb = new StringBuilder(KEY_PREFIX)
                .append(rateLimit.keyType().name().toLowerCase()).append(':')
                .append(keyValue);
        if (rateLimit.key() != null && !rateLimit.key().isEmpty()) {
            sb.append(':').append(rateLimit.key());
        }
        sb.append(':').append(signature.getDeclaringType().getSimpleName())
                .append('#').append(signature.getName());
        return sb.toString();
    }

    /** 根据限流维度解析 key 的可变部分 */
    private String resolveKeyValue(RateLimitKeyType keyType) {
        return switch (keyType) {
            case GLOBAL -> "*";
            case USER_ID -> {
                try {
                    if (StpUtil.isLogin()) {
                        yield String.valueOf(StpUtil.getLoginIdAsLong());
                    }
                } catch (Exception ignored) {
                    // 未登录 / Token 异常：退化为 IP
                }
                yield getClientIp();
            }
            case IP -> getClientIp();
        };
    }

    private String getClientIp() {
        HttpServletRequest request = currentRequest();
        return request == null ? "unknown" : IpUtil.getClientIp(request);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /** 测试时清空所有计数器 */
    void clearAll() {
        counters.invalidateAll();
    }

    /** 暴露给测试 / 运维：查看缓存大小 */
    long size() {
        counters.cleanUp();
        return counters.estimatedSize();
    }

    /** 缓存 value：计数器 + 过期时长（用于 Expiry 回调） */
    private static final class CounterEntry {
        final AtomicLong counter = new AtomicLong(0);
        final long periodSeconds;

        CounterEntry(long periodSeconds) {
            this.periodSeconds = periodSeconds;
        }
    }
}
