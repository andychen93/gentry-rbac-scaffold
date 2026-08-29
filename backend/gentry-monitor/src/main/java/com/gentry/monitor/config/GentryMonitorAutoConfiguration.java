package com.gentry.monitor.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * gentry-monitor 自动装配。{@code gentry.monitor.enabled=false} 时不装配 Redis 监控域。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gentry.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("com.gentry.monitor")
public class GentryMonitorAutoConfiguration {
}
