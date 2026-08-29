package com.gentry.monitor.redis.vo;

/**
 * 预定义 Redis Key 模板 VO（供监控页面用于快速过滤）。
 */
public class RedisKeyDefineVO {
    /** Key 类型标识，如 token_mapping、jwt_blacklist */
    private String keyType;
    /** Key 模板（含通配符），如 satoken:login:token:* */
    private String keyTemplate;
    /** 说明 */
    private String description;
    /** 典型 TTL（秒，-2 表示无 TTL） */
    private Long timeout;

    public RedisKeyDefineVO() {}

    public RedisKeyDefineVO(String keyType, String keyTemplate, String description, Long timeout) {
        this.keyType = keyType;
        this.keyTemplate = keyTemplate;
        this.description = description;
        this.timeout = timeout;
    }

    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }

    public String getKeyTemplate() { return keyTemplate; }
    public void setKeyTemplate(String keyTemplate) { this.keyTemplate = keyTemplate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getTimeout() { return timeout; }
    public void setTimeout(Long timeout) { this.timeout = timeout; }
}
