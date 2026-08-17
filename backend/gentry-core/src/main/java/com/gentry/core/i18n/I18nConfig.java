package com.gentry.core.i18n;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 国际化装配。
 *
 * <p>bean 名必须是 {@code localeResolver}——{@link DispatcherServlet} 按这个固定名字
 * 查找 {@code LocaleResolver}，改名会导致静默不生效。</p>
 */
@Configuration
public class I18nConfig {

    /**
     * 只服务未经过 {@code /api/**} 拦截器的请求（actuator、静态资源）。
     * 已登录请求的语言由 {@code SaTokenConfig} 的 UserContext 拦截器显式设置。
     *
     * <p>{@code gentry.i18n.enabled=false} 时不注册，行为退回 Spring 默认的
     * {@code AcceptHeaderLocaleResolver}。</p>
     */
    @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
    @ConditionalOnProperty(prefix = "gentry.i18n", name = "enabled", matchIfMissing = true)
    public GentryLocaleResolver localeResolver(I18nProperties props) {
        return new GentryLocaleResolver(props);
    }

    /**
     * 让 Bean Validation 的 {@code message = "{valid.xxx}"} 走 Spring 的 MessageSource。
     *
     * <p>只需 {@code setValidationMessageSource} 这一行，131 处校验注解就无需各自调
     * {@code I18nUtil}。约束属性插值（{@code @Size} 的 {@code {max}}）仍由 Hibernate
     * Validator 自己完成，与 MessageSource 无关，所以译文里可以直接写
     * {@code must not exceed {max} characters}。</p>
     *
     * <p><b>没有兜底机制</b>：key 不存在时 Bean Validation 会把 <code>{valid.xxx}</code>
     * 原样输出给用户看，不像 {@code I18nUtil} 有 defaultValue 可退。因此
     * {@code ValidationMessageConstraintTest} 必须扫描全量 key，那是唯一防线。</p>
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource);
        return factory;
    }
}
