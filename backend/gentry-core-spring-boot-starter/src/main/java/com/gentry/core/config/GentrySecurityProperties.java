package com.gentry.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全拦截扩展点：声明哪些 {@code /api/**} 路径不走 Sa-Token 登录校验。
 *
 * <p><b>动机</b>：{@link SaTokenConfig} 的 {@code SaInterceptor} 拦截全部
 * {@code /api/**}，与 Spring Security 的 {@code SecurityFilterChain}（如
 * gentry-oidc-spring-boot-starter 的 Keycloak Resource Server 保护路径）分属
 * 两层不同的拦截机制：请求先过 Security filterChain，再过 Spring MVC
 * HandlerInterceptor。若某路径已由其它认证机制保护（如 OAuth2 JWT），
 * Sa-Token 拦截器仍会因为没有 Sa-Token session 而返回 401，造成双重拦截冲突。</p>
 *
 * <pre>
 * gentry:
 *   security:
 *     sa-token-exclude-paths:
 *       - /api/sso/**
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "gentry.security")
public class GentrySecurityProperties {

    /**
     * 不走 Sa-Token 登录校验的路径模式列表（Ant 风格），
     * 消费方需为这些路径自行提供等价的认证保护（如 OAuth2 Resource Server）。
     */
    private List<String> saTokenExcludePaths = new ArrayList<>();

    public List<String> getSaTokenExcludePaths() {
        return saTokenExcludePaths;
    }

    public void setSaTokenExcludePaths(List<String> saTokenExcludePaths) {
        this.saTokenExcludePaths = saTokenExcludePaths;
    }
}
