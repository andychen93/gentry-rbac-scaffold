package com.gentry.rbac.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * gentry-rbac 自动装配：RBAC + 通知域的组件与 Mapper。
 *
 * <p>{@code gentry.rbac.enabled=false} 时不装配整个域（只引 core 做横切的项目用）。
 * MapperScan 从原 GentryApplication 主类迁移至此；消费方主类不再需要 @MapperScan。
 * 与主类上遗留的 {@code @MapperScan("com.gentry.**.mapper")} 并存时，
 * Spring 按接口 FQN 同名去重，无冲突。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gentry.rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan({"com.gentry.rbac", "com.gentry.notification"})
@MapperScan({"com.gentry.rbac.**.mapper", "com.gentry.notification.**.mapper"})
public class GentryRbacAutoConfiguration {
}
