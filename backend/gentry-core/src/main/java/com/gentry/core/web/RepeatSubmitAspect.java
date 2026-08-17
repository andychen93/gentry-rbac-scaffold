package com.gentry.core.web;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.exception.BizException;
import com.gentry.core.util.IpUtil;
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
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 重复提交防护切面。
 *
 * <p>基于 Caffeine 本地缓存存储请求指纹，在 {@code interval} 秒内拒绝相同指纹的重复请求。
 * 指纹由 {@code userId|method|uri|paramsMD5} 组成，保证不同用户互不干扰。</p>
 */
@Aspect
@Component
public class RepeatSubmitAspect {

    private static final Logger log = LoggerFactory.getLogger(RepeatSubmitAspect.class);

    private static final String KEY_PREFIX = "repeat_submit:";

    private static final long MAX_SIZE = 10_000L;

    private final Cache<String, Long> submitCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RepeatSubmitAspect() {
        this.submitCache = Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfter(new Expiry<String, Long>() {
                    @Override
                    public long expireAfterCreate(String key, Long value, long currentTime) {
                        return TimeUnit.SECONDS.toNanos(value);
                    }

                    @Override
                    public long expireAfterUpdate(String key, Long value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, Long value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        if (repeatSubmit.interval() <= 0) {
            return pjp.proceed();
        }

        String fingerprint = buildFingerprint(pjp);
        String cacheKey = KEY_PREFIX + fingerprint;

        Long existing = submitCache.getIfPresent(cacheKey);
        if (existing != null) {
            // 注解上的 message 现在是 i18n key（可选）；为空则用 ErrorCode 派生的 key
            String messageKey = repeatSubmit.message() == null || repeatSubmit.message().isEmpty()
                    ? null
                    : repeatSubmit.message();
            log.warn("重复提交拦截: key={}, interval={}s", cacheKey, repeatSubmit.interval());
            throw new BizException(ErrorCode.DUPLICATE_SUBMIT, messageKey);
        }

        submitCache.put(cacheKey, (long) repeatSubmit.interval());
        return pjp.proceed();
    }

    /** 请求指纹：userId | method | uri | args MD5 */
    String buildFingerprint(ProceedingJoinPoint pjp) {
        HttpServletRequest request = currentRequest();
        String userKey = resolveUserKey(request);
        String method = request == null ? "UNKNOWN" : request.getMethod();
        String uri = request == null ? pjp.getSignature().toShortString() : request.getRequestURI();
        String paramsMd5 = md5(extractArgs(pjp));
        return userKey + "|" + method + "|" + uri + "|" + paramsMd5;
    }

    /** 提取方法参数并序列化为 JSON；跳过不可序列化对象（HttpServletRequest / MultipartFile 等） */
    String extractArgs(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return "";
        Object[] filtered = Arrays.stream(args)
                .filter(this::isSerializable)
                .toArray();
        if (filtered.length == 0) {
            MethodSignature ms = (MethodSignature) pjp.getSignature();
            return ms.getMethod().getName();
        }
        try {
            return objectMapper.writeValueAsString(filtered);
        } catch (JsonProcessingException e) {
            // 兜底：序列化失败时用 toString
            return Arrays.deepToString(filtered);
        }
    }

    private boolean isSerializable(Object arg) {
        if (arg == null) return false;
        if (arg instanceof HttpServletRequest) return false;
        if (arg instanceof jakarta.servlet.http.HttpServletResponse) return false;
        if (arg instanceof MultipartFile) return false;
        return true;
    }

    private String resolveUserKey(HttpServletRequest request) {
        try {
            if (StpUtil.isLogin()) {
                return "u:" + StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return "ip:" + (request == null ? "unknown" : IpUtil.getClientIp(request));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    static String md5(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }

    /** 测试方法：清空缓存 */
    void clearAll() {
        submitCache.invalidateAll();
    }

    long size() {
        submitCache.cleanUp();
        return submitCache.estimatedSize();
    }
}
