package com.gentry.core.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("全链路追踪过滤器")
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("无上游 traceId 时自动生成 UUID 并写入 MDC / 响应头")
    void generateUuidWhenAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // 过滤器 finally 会清理 MDC，我们通过响应头断言
        String traceId = response.getHeader(TraceIdFilter.HEADER_TRACE_ID);
        assertThat(traceId).isNotNull().hasSize(32);
    }

    @Test
    @DisplayName("请求头带 X-Trace-Id 时透传")
    void propagatesExistingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_TRACE_ID, "abc-1234-upstream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdFilter.HEADER_TRACE_ID)).isEqualTo("abc-1234-upstream");
    }

    @Test
    @DisplayName("请求处理期间 MDC 中有 traceId，结束后清理")
    void mdcIsSetDuringChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] captured = new String[1];
        FilterChain chain = (req, resp) -> captured[0] = TraceContext.getTraceId();

        filter.doFilter(request, response, chain);

        assertThat(captured[0]).isNotNull().hasSize(32);
        assertThat(TraceContext.getTraceId()).isNull();
    }

    @Test
    @DisplayName("异常也会清理 MDC")
    void mdcClearedEvenOnException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, resp) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (IllegalStateException ignored) {
        }
        assertThat(TraceContext.getTraceId()).isNull();
    }

    @Test
    @DisplayName("newUuidTraceId 返回 32 位无分隔符串")
    void newUuidTraceId_32chars() {
        String id = TraceIdFilter.newUuidTraceId();
        assertThat(id).hasSize(32).matches("[0-9a-f]+");
    }
}
