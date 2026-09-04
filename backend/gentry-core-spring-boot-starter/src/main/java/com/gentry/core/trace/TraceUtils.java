package com.gentry.core.trace;

import java.util.concurrent.Callable;

/**
 * 异步任务的 traceId 传递工具。
 *
 * <p>使用示例：</p>
 * <pre>
 * executor.submit(TraceUtils.wrap(() -&gt; doWork()));
 * executor.submit(TraceUtils.wrap(() -&gt; computeResult()));
 * </pre>
 */
public final class TraceUtils {

    private TraceUtils() {
    }

    public static Runnable wrap(Runnable delegate) {
        String traceId = TraceContext.getTraceId();
        return () -> {
            String prev = TraceContext.getTraceId();
            TraceContext.setTraceId(traceId);
            try {
                delegate.run();
            } finally {
                if (prev == null) {
                    TraceContext.clear();
                } else {
                    TraceContext.setTraceId(prev);
                }
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> delegate) {
        String traceId = TraceContext.getTraceId();
        return () -> {
            String prev = TraceContext.getTraceId();
            TraceContext.setTraceId(traceId);
            try {
                return delegate.call();
            } finally {
                if (prev == null) {
                    TraceContext.clear();
                } else {
                    TraceContext.setTraceId(prev);
                }
            }
        };
    }
}
