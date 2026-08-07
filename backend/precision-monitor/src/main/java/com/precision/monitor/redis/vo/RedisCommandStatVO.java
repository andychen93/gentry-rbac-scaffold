package com.precision.monitor.redis.vo;

/**
 * Redis 命令统计 VO（来自 INFO commandstats）。
 */
public class RedisCommandStatVO {
    /** 命令名，如 get/set/expire */
    private String name;
    /** 总调用次数 */
    private Long calls;
    /** 累计耗时（微秒） */
    private Long usec;
    /** 平均每次耗时（微秒） */
    private Long usecPerCall;

    public RedisCommandStatVO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getCalls() { return calls; }
    public void setCalls(Long calls) { this.calls = calls; }

    public Long getUsec() { return usec; }
    public void setUsec(Long usec) { this.usec = usec; }

    public Long getUsecPerCall() { return usecPerCall; }
    public void setUsecPerCall(Long usecPerCall) { this.usecPerCall = usecPerCall; }
}
