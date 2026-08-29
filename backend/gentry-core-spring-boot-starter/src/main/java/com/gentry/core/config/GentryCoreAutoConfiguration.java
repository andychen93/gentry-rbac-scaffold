package com.gentry.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * gentry-core 自动装配入口。
 *
 * <p>starter 化前，core 的 Bean 依赖消费方主类放在 {@code com.gentry} 根包下被包扫描兜住；
 * starter 化后由本类定向扫描注册，消费方主类可在任意包。
 * core 内配置类之间有构造注入依赖（如 SaTokenConfig ← GentryLocaleResolver 的
 * ObjectProvider），逐个转 @Bean 声明容易制造循环，定向 ComponentScan 保持原语义。</p>
 */
@AutoConfiguration
@ComponentScan("com.gentry.core")
public class GentryCoreAutoConfiguration {
}
