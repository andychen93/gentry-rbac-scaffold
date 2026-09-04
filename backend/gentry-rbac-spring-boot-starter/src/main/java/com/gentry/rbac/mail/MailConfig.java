package com.gentry.rbac.mail;

import com.gentry.rbac.config.GentryRegisterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件基础设施配置（对齐 {@code NotificationConfig} 的网关桩模式）。
 * 顺带注册自助注册配置（同属邮箱认证域）。
 */
@Configuration
@EnableConfigurationProperties({GentryMailProperties.class, GentryRegisterProperties.class})
public class MailConfig {

    /** 默认邮件网关为日志桩；启用 SMTP 或消费方自定义网关时自动覆盖。 */
    @Bean
    @ConditionalOnMissingBean(MailGateway.class)
    public MailGateway loggingMailGateway() {
        return new LoggingMailGateway();
    }
}
