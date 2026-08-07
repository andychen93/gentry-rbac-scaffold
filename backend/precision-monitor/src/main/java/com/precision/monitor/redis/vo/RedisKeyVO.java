package com.precision.monitor.redis.vo;

/**
 * Redis 单个 Key 详情 VO。
 */
public class RedisKeyVO {
    /** Key 名 */
    private String key;
    /** Key 类型 string/hash/list/set/zset */
    private String type;
    /** 值（格式化后的字符串，截断到 500 字符） */
    private String value;
    /** 剩余秒数，-1 永不过期，-2 已过期/不存在 */
    private Long ttl;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Long getTtl() { return ttl; }
    public void setTtl(Long ttl) { this.ttl = ttl; }
}
