package com.precision.core.trace;

import org.slf4j.MDC;

/**
 * 全链路追踪上下文：基于 SLF4J {@link MDC} 维护当前线程的 traceId。
 *
 * <p>设计要点：</p>
 * <ul>
 *     <li>MDC 底层基于 ThreadLocal，天然线程隔离</li>
 *     <li>统一通过 MDC 而非额外 ThreadLocal，避免 logback 的 {@code %X{traceId}} 与代码读取不一致</li>
 *     <li>TraceContext 仅是语义封装，不独立持有状态</li>
 * </ul>
 */
public final class TraceContext {

    /** MDC 中的 key */
    public static final String TRACE_ID_KEY = "traceId";

    private TraceContext() {
    }

    /** 获取当前线程的 traceId，未设置返回 null */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /** 设置当前线程的 traceId */
    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            MDC.remove(TRACE_ID_KEY);
        } else {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /** 清理当前线程的 traceId（请求结束时调用） */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
