package com.precision.monitor.redis.service;

import com.precision.core.common.PageResult;
import com.precision.monitor.redis.vo.RedisKeyVO;

/**
 * Redis Key 查询与操作服务。
 */
public interface RedisKeyService {

    /**
     * 使用 SCAN 按模式扫描 Key 并分页返回。
     *
     * @param pattern  匹配模式，例如 {@code blacklist:*}
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页大小（建议 ≤ 100）
     */
    PageResult<RedisKeyVO> scanKeys(String pattern, int pageNum, int pageSize);

    /**
     * 获取指定 Key 的详情（类型 + 值 + TTL）。值会被截断到 500 字符。
     */
    RedisKeyVO getKeyValue(String key);

    /**
     * 删除指定 Key。
     *
     * @return true 表示实际删除了 Key，false 表示 Key 不存在
     */
    boolean deleteKey(String key);
}
