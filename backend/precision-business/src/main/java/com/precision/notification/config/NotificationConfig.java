package com.precision.notification.config;

import com.precision.notification.sms.LoggingSmsGateway;
import com.precision.notification.sms.SmsGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知基础设施配置。
 */
@Configuration
public class NotificationConfig {

    /** 默认短信网关为日志桩；接入真实网关时提供同类型 Bean 即自动覆盖。 */
    @Bean
    @ConditionalOnMissingBean(SmsGateway.class)
    public SmsGateway loggingSmsGateway() {
        return new LoggingSmsGateway();
    }
}
