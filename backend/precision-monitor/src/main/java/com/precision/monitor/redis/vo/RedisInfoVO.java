package com.precision.monitor.redis.vo;

/**
 * Redis 基本运行状态 VO（来自 INFO 命令解析）。
 */
public class RedisInfoVO {
    /** Redis 版本号 */
    private String redisVersion;
    /** 运行模式 standalone/sentinel/cluster */
    private String redisMode;
    /** 运行时长（秒） */
    private Long uptimeInSeconds;
    /** 已连接客户端数 */
    private Integer connectedClients;
    /** 已用内存（字节） */
    private Long usedMemory;
    /** 最大内存（字节，0 代表无限制） */
    private Long maxMemory;
    /** 内存使用率（%，精确到小数点后两位） */
    private Double usedMemoryPercent;
    /** 当前库 Key 总数 */
    private Long totalKeys;
    /** 设置过期时间的 Key 数 */
    private Long expiresKeys;
    /** 平均 TTL（毫秒） */
    private Long avgTtl;
    /** AOF 是否开启（0/1） */
    private String aofEnabled;
    /** 最近一次 RDB 保存时间戳 */
    private String rdbLastSaveTime;

    // ============ 扩展指标（stats + clients 节）============

    /** 自启动以来的总命令数 */
    private Long totalCommandsProcessed;
    /** 自启动以来的总连接数 */
    private Long totalConnectionsReceived;
    /** 瞬时每秒命令数（ops/sec） */
    private Long instantaneousOpsPerSec;
    /** 累计命中次数 */
    private Long keyspaceHits;
    /** 累计未命中次数 */
    private Long keyspaceMisses;
    /** 命中率（%，精确到两位小数） */
    private Double hitRate;
    /** 订阅频道数 */
    private Integer pubsubChannels;
    /** 订阅模式数 */
    private Integer pubsubPatterns;

    public String getRedisVersion() { return redisVersion; }
    public void setRedisVersion(String redisVersion) { this.redisVersion = redisVersion; }

    public String getRedisMode() { return redisMode; }
    public void setRedisMode(String redisMode) { this.redisMode = redisMode; }

    public Long getUptimeInSeconds() { return uptimeInSeconds; }
    public void setUptimeInSeconds(Long uptimeInSeconds) { this.uptimeInSeconds = uptimeInSeconds; }

    public Integer getConnectedClients() { return connectedClients; }
    public void setConnectedClients(Integer connectedClients) { this.connectedClients = connectedClients; }

    public Long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(Long usedMemory) { this.usedMemory = usedMemory; }

    public Long getMaxMemory() { return maxMemory; }
    public void setMaxMemory(Long maxMemory) { this.maxMemory = maxMemory; }

    public Double getUsedMemoryPercent() { return usedMemoryPercent; }
    public void setUsedMemoryPercent(Double usedMemoryPercent) { this.usedMemoryPercent = usedMemoryPercent; }

    public Long getTotalKeys() { return totalKeys; }
    public void setTotalKeys(Long totalKeys) { this.totalKeys = totalKeys; }

    public Long getExpiresKeys() { return expiresKeys; }
    public void setExpiresKeys(Long expiresKeys) { this.expiresKeys = expiresKeys; }

    public Long getAvgTtl() { return avgTtl; }
    public void setAvgTtl(Long avgTtl) { this.avgTtl = avgTtl; }

    public String getAofEnabled() { return aofEnabled; }
    public void setAofEnabled(String aofEnabled) { this.aofEnabled = aofEnabled; }

    public String getRdbLastSaveTime() { return rdbLastSaveTime; }
    public void setRdbLastSaveTime(String rdbLastSaveTime) { this.rdbLastSaveTime = rdbLastSaveTime; }

    public Long getTotalCommandsProcessed() { return totalCommandsProcessed; }
    public void setTotalCommandsProcessed(Long totalCommandsProcessed) { this.totalCommandsProcessed = totalCommandsProcessed; }

    public Long getTotalConnectionsReceived() { return totalConnectionsReceived; }
    public void setTotalConnectionsReceived(Long totalConnectionsReceived) { this.totalConnectionsReceived = totalConnectionsReceived; }

    public Long getInstantaneousOpsPerSec() { return instantaneousOpsPerSec; }
    public void setInstantaneousOpsPerSec(Long instantaneousOpsPerSec) { this.instantaneousOpsPerSec = instantaneousOpsPerSec; }

    public Long getKeyspaceHits() { return keyspaceHits; }
    public void setKeyspaceHits(Long keyspaceHits) { this.keyspaceHits = keyspaceHits; }

    public Long getKeyspaceMisses() { return keyspaceMisses; }
    public void setKeyspaceMisses(Long keyspaceMisses) { this.keyspaceMisses = keyspaceMisses; }

    public Double getHitRate() { return hitRate; }
    public void setHitRate(Double hitRate) { this.hitRate = hitRate; }

    public Integer getPubsubChannels() { return pubsubChannels; }
    public void setPubsubChannels(Integer pubsubChannels) { this.pubsubChannels = pubsubChannels; }

    public Integer getPubsubPatterns() { return pubsubPatterns; }
    public void setPubsubPatterns(Integer pubsubPatterns) { this.pubsubPatterns = pubsubPatterns; }
}
