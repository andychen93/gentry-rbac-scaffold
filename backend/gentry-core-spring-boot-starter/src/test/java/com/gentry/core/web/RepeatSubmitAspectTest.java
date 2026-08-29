package com.gentry.core.web;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("重复提交防护切面")
class RepeatSubmitAspectTest {

    private RepeatSubmitAspect aspect;

    @BeforeEach
    void setup() {
        aspect = new RepeatSubmitAspect();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/users");
        request.setRemoteAddr("192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("首次请求正常通过")
    void firstRequest_passesThrough() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("create", new Object[]{"payload"});
        Object result = aspect.around(pjp, submitIn(3, ""));
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("间隔内相同参数的重复提交被拒绝")
    void sameArgs_duplicateRejected() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("create", new Object[]{"same-payload"});

        aspect.around(pjp, submitIn(5, ""));

        assertThatThrownBy(() -> aspect.around(pjp, submitIn(5, "")))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.DUPLICATE_SUBMIT.getCode());
    }

    @Test
    @DisplayName("不同参数不拦截（指纹不同）")
    void differentArgs_bothPass() throws Throwable {
        Object r1 = aspect.around(mockPjp("create", new Object[]{"a"}), submitIn(5, ""));
        Object r2 = aspect.around(mockPjp("create", new Object[]{"b"}), submitIn(5, ""));
        assertThat(r1).isEqualTo("ok");
        assertThat(r2).isEqualTo("ok");
    }

    @Test
    @DisplayName("不同 URI 不拦截")
    void differentUri_bothPass() throws Throwable {
        ProceedingJoinPoint pjp1 = mockPjp("create", new Object[]{"x"});
        aspect.around(pjp1, submitIn(5, ""));

        // 切换 URI
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setMethod("POST");
        req2.setRequestURI("/api/v1/roles");
        req2.setRemoteAddr("192.168.1.100");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req2));

        ProceedingJoinPoint pjp2 = mockPjp("create", new Object[]{"x"});
        Object r = aspect.around(pjp2, submitIn(5, ""));
        assertThat(r).isEqualTo("ok");
    }

    @Test
    @DisplayName("自定义 message 被使用")
    void customMessage_used() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("create", new Object[]{"payload"});
        aspect.around(pjp, submitIn(5, ""));

        assertThatThrownBy(() -> aspect.around(pjp, submitIn(5, "别再点了")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("别再点了");
    }

    @Test
    @DisplayName("interval <= 0 时直接放行")
    void zeroInterval_noCheck() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("create", new Object[]{"payload"});
        for (int i = 0; i < 10; i++) {
            Object result = aspect.around(pjp, submitIn(0, ""));
            assertThat(result).isEqualTo("ok");
        }
    }

    @Test
    @DisplayName("MD5 工具：相同输入产生相同哈希")
    void md5_deterministic() {
        assertThat(RepeatSubmitAspect.md5("hello"))
                .isEqualTo(RepeatSubmitAspect.md5("hello"))
                .isNotEqualTo(RepeatSubmitAspect.md5("world"));
    }

    @Test
    @DisplayName("buildFingerprint 包含 userKey / method / uri / paramsMd5")
    void buildFingerprint_structure() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp("create", new Object[]{"payload"});
        String fp = aspect.buildFingerprint(pjp);
        assertThat(fp).contains("|POST|/api/v1/users|");
        assertThat(fp.split("\\|")).hasSize(4);
    }

    // ============ helpers ============

    private ProceedingJoinPoint mockPjp(String name, Object[] args) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getName()).thenReturn(name);
        when(signature.getDeclaringType()).thenReturn((Class) Object.class);
        when(signature.toShortString()).thenReturn("Dummy." + name);
        Method m = Object.class.getMethods()[0];
        when(signature.getMethod()).thenReturn(m);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(args);
        when(pjp.proceed()).thenReturn("ok");
        return pjp;
    }

    private RepeatSubmit submitIn(int interval, String msg) {
        return new RepeatSubmit() {
            @Override public Class<? extends Annotation> annotationType() { return RepeatSubmit.class; }
            @Override public int interval() { return interval; }
            @Override public String message() { return msg; }
        };
    }
}
