package com.precision.core.web;

import com.precision.core.common.ErrorCode;
import com.precision.core.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 接口限流切面单元测试。
 */
@DisplayName("接口限流切面")
class RateLimitAspectTest {

    private RateLimitAspect aspect;

    @BeforeEach
    void setup() {
        aspect = new RateLimitAspect();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("阈值内的请求正常通过")
    void underLimit_passesThrough() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("handle");
        RateLimit annotation = rateLimit(3, 60, RateLimitKeyType.IP);

        for (int i = 0; i < 3; i++) {
            Object result = aspect.around(pjp, annotation);
            assertThat(result).isEqualTo("ok");
        }
    }

    @Test
    @DisplayName("超过阈值抛 TOO_MANY_REQUESTS")
    void overLimit_throwsTooManyRequests() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("handle");
        RateLimit annotation = rateLimit(2, 60, RateLimitKeyType.IP);

        aspect.around(pjp, annotation);
        aspect.around(pjp, annotation);

        assertThatThrownBy(() -> aspect.around(pjp, annotation))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.TOO_MANY_REQUESTS.getCode());
    }

    @Test
    @DisplayName("自定义 message 生效")
    void customMessage_used() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("handle");
        RateLimit annotation = rateLimitWithMsg(1, 60, RateLimitKeyType.IP, "太快啦");

        aspect.around(pjp, annotation);
        assertThatThrownBy(() -> aspect.around(pjp, annotation))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("太快啦");
    }

    @Test
    @DisplayName("不同 IP 互不干扰")
    void differentIp_isolated() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("handle");
        RateLimit annotation = rateLimit(1, 60, RateLimitKeyType.IP);

        aspect.around(pjp, annotation);    // IP1 第 1 次
        // 切换 IP
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request2));

        Object result = aspect.around(pjp, annotation);  // IP2 第 1 次，应通过
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("GLOBAL 维度所有请求共用一个计数器")
    void globalKeyType_sharedCounter() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("handle");
        RateLimit annotation = rateLimit(1, 60, RateLimitKeyType.GLOBAL);

        aspect.around(pjp, annotation);    // IP1 请求
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("10.0.0.5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request2));

        assertThatThrownBy(() -> aspect.around(pjp, annotation))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("count=0 / period=0 时直接放行，不做限流")
    void zeroLimits_noThrottling() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("handle");
        RateLimit annotation = rateLimit(0, 60, RateLimitKeyType.IP);

        for (int i = 0; i < 100; i++) {
            Object result = aspect.around(pjp, annotation);
            assertThat(result).isEqualTo("ok");
        }
    }

    @Test
    @DisplayName("buildKey 包含 keyType/keyValue/方法签名")
    void buildKey_containsAllParts() {
        RateLimit annotation = rateLimit(10, 60, RateLimitKeyType.IP);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getName()).thenReturn("handle");
        when(signature.getDeclaringType()).thenReturn((Class) String.class);

        String key = aspect.buildKey(annotation, signature);

        assertThat(key)
                .contains("rate_limit:")
                .contains("ip")
                .contains("192.168.1.100")
                .contains("String#handle");
    }

    // ============ helpers ============

    private ProceedingJoinPoint mockPjp(String methodName) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    private RateLimit rateLimit(int count, int period, RateLimitKeyType type) {
        return rateLimitWithMsg(count, period, type, "");
    }

    private RateLimit rateLimitWithMsg(int count, int period, RateLimitKeyType type, String msg) {
        return new RateLimit() {
            @Override public Class<? extends Annotation> annotationType() { return RateLimit.class; }
            @Override public String key() { return ""; }
            @Override public RateLimitKeyType keyType() { return type; }
            @Override public int count() { return count; }
            @Override public int period() { return period; }
            @Override public String message() { return msg; }
        };
    }
}
