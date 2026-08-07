package com.precision.monitor.redis.vo;

import java.util.List;

/**
 * Redis 慢查询日志条目 VO。
 * <p>来源：{@code SLOWLOG GET [n]} 命令。</p>
 */
public class RedisSlowLogVO {
    /** 慢日志唯一 ID（递增） */
    private Long id;
    /** 命令执行时的 Unix 时间戳（秒） */
    private Long timestamp;
    /** 执行耗时（微秒） */
    private Long durationMicros;
    /** 命令片段（已拼接为字符串，较长参数会被截断） */
    private List<String> args;
    /** 客户端地址（Redis 4.0+） */
    private String clientAddress;
    /** 客户端名称（Redis 4.0+，CLIENT SETNAME 设置） */
    private String clientName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Long getDurationMicros() { return durationMicros; }
    public void setDurationMicros(Long durationMicros) { this.durationMicros = durationMicros; }

    public List<String> getArgs() { return args; }
    public void setArgs(List<String> args) { this.args = args; }

    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
}
