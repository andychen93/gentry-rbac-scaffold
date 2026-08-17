package com.gentry.core.i18n;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
