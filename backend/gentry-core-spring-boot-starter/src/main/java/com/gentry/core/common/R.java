package com.gentry.core.common;

import com.gentry.core.trace.TraceContext;

import java.io.Serializable;

/**
 * 统一响应包装
 */
public class R<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;

    public R() {
        this.timestamp = System.currentTimeMillis();
        // 自动填充当前线程的 traceId，便于前端定位
        this.traceId = TraceContext.getTraceId();
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        R<T> r = new R<>();
        r.code = errorCode.getCode();
        r.message = message;
        return r;
    }

    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
