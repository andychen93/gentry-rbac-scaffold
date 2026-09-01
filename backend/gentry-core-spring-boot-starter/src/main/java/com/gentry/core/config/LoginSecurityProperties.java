package com.gentry.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录安全策略配置（账号连续失败锁定）。
 *
 * <pre>
 * gentry:
 *   login-security:
 *     max-fail-count: 5
 *     lock-minutes: 10
 * </pre>
 *
 * <p>计数基于 Redis（{@code login_fail:{username}}），多节点部署一致。</p>
 */
@Component
@ConfigurationProperties(prefix = "gentry.login-security")
public class LoginSecurityProperties {

    /** 连续登录失败 N 次后锁定账号 */
    private int maxFailCount = 5;

    /** 锁定时长（分钟），即失败计数器的 Redis TTL */
    private int lockMinutes = 10;

    public int getMaxFailCount() { return maxFailCount; }
    public void setMaxFailCount(int maxFailCount) { this.maxFailCount = maxFailCount; }

    public int getLockMinutes() { return lockMinutes; }
    public void setLockMinutes(int lockMinutes) { this.lockMinutes = lockMinutes; }
}
