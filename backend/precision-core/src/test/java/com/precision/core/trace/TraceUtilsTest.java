package com.precision.core.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("追踪上下文异步传递")
class TraceUtilsTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("Runnable 在另一线程中能看到原始 traceId")
    void wrapRunnable_propagates() throws Exception {
        TraceContext.setTraceId("trace-R-1");
        String[] captured = new String[1];

        Runnable r = TraceUtils.wrap((Runnable) () -> captured[0] = TraceContext.getTraceId());
        var executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(r).get();
        } finally {
            executor.shutdown();
        }

        assertThat(captured[0]).isEqualTo("trace-R-1");
    }

    @Test
    @DisplayName("Callable 在另一线程中能看到原始 traceId")
    void wrapCallable_propagates() throws Exception {
        TraceContext.setTraceId("trace-C-1");
        Callable<String> wrapped = TraceUtils.wrap(() -> TraceContext.getTraceId());
        var executor = Executors.newSingleThreadExecutor();
        try {
            String result = executor.submit(wrapped).get();
            assertThat(result).isEqualTo("trace-C-1");
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("执行完恢复上一个 traceId（嵌套场景）")
    void wrapRunnable_restoresPrevious() {
        TraceContext.setTraceId("outer");
        Runnable inner = TraceUtils.wrap(() -> {
            // inner 线程开始时 traceId 应为 "outer"（wrap 时捕获的），
            // 这里模拟 inner 临时改写并确认 wrap 内部 finally 会恢复
            TraceContext.setTraceId("inner-changed");
        });
        inner.run();
        assertThat(TraceContext.getTraceId()).isEqualTo("outer");
    }
}
