package com.gentry.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * gentry-core 自动装配入口。
 *
 * <p>starter 化前，core 的 Bean 依赖消费方主类放在 {@code com.gentry} 根包下被包扫描兜住；
 * starter 化后由本类定向扫描注册，消费方主类可在任意包。
 * core 内配置类之间有构造注入依赖（如 SaTokenConfig ← GentryLocaleResolver 的
 * ObjectProvider），逐个转 @Bean 声明容易制造循环，定向 ComponentScan 保持原语义。</p>
 *
 * <p>{@link AutoConfigureBefore}({@link WebMvcAutoConfiguration})：I18nConfig 的
 * {@code localeResolver} 与 Boot WebMvc 的同名条件 Bean 竞争，必须让 core 先注册、
 * Boot 侧 {@code @ConditionalOnMissingBean} 让路。starter 化前 I18nConfig 走主类
 * 包扫描天然位于自动配置之前；starter 化后进入自动配置序列，需显式声明该顺序。</p>
 */
@AutoConfiguration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@EnableScheduling
@ComponentScan("com.gentry.core")
public class GentryCoreAutoConfiguration {
}
