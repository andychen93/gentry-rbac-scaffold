package com.precision.monitor.redis.service;

import com.precision.monitor.redis.vo.RedisKeyDefineVO;
import com.precision.monitor.redis.vo.RedisMonitorVO;
import com.precision.monitor.redis.vo.RedisSlowLogVO;

import java.util.List;

/**
 * Redis 监控服务。
 */
public interface RedisMonitorService {

    /**
     * 获取 Redis 监控总览（INFO + dbSize + 命令统计）。
     */
    RedisMonitorVO getMonitorInfo();

    /**
     * 获取系统中预定义的 Redis Key 模板列表。
     */
    List<RedisKeyDefineVO> getKeyDefines();

    /**
     * 获取慢查询日志（最近 N 条，按耗时降序）。
     *
     * @param limit 返回条数，1~200
     */
    List<RedisSlowLogVO> getSlowLog(int limit);

    /**
     * 清空慢查询日志（{@code SLOWLOG RESET}）。
     */
    void resetSlowLog();
}
