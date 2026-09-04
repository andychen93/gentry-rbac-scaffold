package com.gentry.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 密码策略配置。
 *
 * <pre>
 * gentry:
 *   password:
 *     expire-days: 90   # 0 或负数表示不校验过期
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "gentry.password")
public class PasswordProperties {

    /** 密码过期天数；0 或负数表示不校验 */
    private int expireDays = 90;

    public int getExpireDays() { return expireDays; }
    public void setExpireDays(int expireDays) { this.expireDays = expireDays; }
}
