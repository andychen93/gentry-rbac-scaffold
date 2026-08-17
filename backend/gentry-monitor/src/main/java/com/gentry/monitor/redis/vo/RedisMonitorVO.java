package com.gentry.monitor.redis.vo;

import java.util.List;

/**
 * Redis 监控总览 VO（info + dbSize + commandStats 一次返回）。
 */
public class RedisMonitorVO {
    private RedisInfoVO info;
    private Long dbSize;
    private List<RedisCommandStatVO> commandStats;

    public RedisInfoVO getInfo() { return info; }
    public void setInfo(RedisInfoVO info) { this.info = info; }

    public Long getDbSize() { return dbSize; }
    public void setDbSize(Long dbSize) { this.dbSize = dbSize; }

    public List<RedisCommandStatVO> getCommandStats() { return commandStats; }
    public void setCommandStats(List<RedisCommandStatVO> commandStats) { this.commandStats = commandStats; }
}
