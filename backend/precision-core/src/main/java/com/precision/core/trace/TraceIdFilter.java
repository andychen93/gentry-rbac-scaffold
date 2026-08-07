package com.precision.core.trace;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * 追踪 ID 过滤器，在请求最前端生成或透传 traceId。
 *
 * <p>生成优先级：</p>
 * <ol>
 *     <li>SkyWalking agent 提供的 traceId（如果 apm-toolkit-trace 在类路径上且 agent 已激活）</li>
 *     <li>请求头 {@code X-Trace-Id}（支持上游服务透传）</li>
 *     <li>UUID（32 位不带横线）</li>
 * </ol>
 *
 * <p>设置位置：MDC / {@link TraceContext} / {@code X-Trace-Id} 响应头。</p>
 */
public class TraceIdFilter implements Filter {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String traceId = generateTraceId(httpReq);
        TraceContext.setTraceId(traceId);
        httpResp.setHeader(HEADER_TRACE_ID, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }

    /** 生成 traceId，按优先级：SkyWalking → 请求头 → UUID */
    String generateTraceId(HttpServletRequest request) {
        String swTraceId = tryGetSkyWalkingTraceId();
        if (swTraceId != null) {
            return swTraceId;
        }
        String existing = request.getHeader(HEADER_TRACE_ID);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        return newUuidTraceId();
    }

    /**
     * 尝试读取 SkyWalking 原生 traceId，类路径不存在或未激活时返回 null。
     */
    String tryGetSkyWalkingTraceId() {
        try {
            // 反射调用 SkyWalking API，避免强依赖
            Class<?> cls = Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            Object result = cls.getMethod("traceId").invoke(null);
            if (result == null) return null;
            String traceId = result.toString();
            if (traceId.isEmpty() || "Ignored_Trace".equals(traceId) || "N/A".equals(traceId)) {
                return null;
            }
            return traceId;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String newUuidTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
