package com.gentry.oidc.config;

import com.gentry.oidc.service.OidcUserSyncService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * OIDC 自动配置。
 * <p>根据 gentry.oidc.mode 配置决定启用 SSO 模式还是本地模式。</p>
 *
 * <p><b>JWT 验证委托给 Spring Security 标准组件</b>：
 * {@link JwtDecoders#fromIssuerLocation(String)} 会自动从 Keycloak 的
 * {@code /.well-known/openid-configuration} 获取 jwks_uri，并按 issuer、
 * signature、exp、nbf 规则验证 token，避免自行实现验签逻辑带来的安全风险。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gentry.oidc", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(OidcProperties.class)
public class OidcAutoConfiguration {

    /**
     * OIDC 用户同步服务 Bean。
     * <p>{@code repository} 为 {@link java.util.Optional}：消费方未提供
     * {@link com.gentry.oidc.service.ExternalIdentityRepository} 实现时，
     * JIT 同步会记录警告日志而非启动失败（符合本 starter 不强绑定
     * rbac-starter 内部表结构的设计）。</p>
     */
    @Bean
    public OidcUserSyncService oidcUserSyncService(
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            java.util.Optional<com.gentry.oidc.service.ExternalIdentityRepository> repository,
            OidcProperties properties) {
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, repository);
        service.setAutoCreateUser(properties.isAutoCreateUser());
        service.setDefaultRole(properties.getDefaultRole());
        return service;
    }

    /**
     * OIDC 安全配置（仅在 SSO 模式下启用）。
     */
    /**
     * 仅在 gentry.oidc.enabled=true 且 gentry.oidc.mode=keycloak 时装配。
     * <p>mode=local 时不装配 OAuth2 Resource Server 过滤器链，由消费方自行
     * 保留 Sa-Token 本地认证（架构约束：两种模式不可同时信任同一 API）。</p>
     */
    @Configuration
    @EnableWebSecurity
    @ConditionalOnProperty(prefix = "gentry.oidc", name = "mode", havingValue = "keycloak", matchIfMissing = true)
    public static class KeycloakSecurityConfig {

        private final OidcProperties properties;

        public KeycloakSecurityConfig(OidcProperties properties) {
            this.properties = properties;
        }

        /**
         * JWT 解码器：issuer 拼接 Keycloak realm 地址，
         * 由 Spring Security 自动发现 jwks_uri 并验证签名、exp、nbf、iss，
         * 并追加 audience 校验（Keycloak 默认 access token 的 aud 为 "account"，
         * 需通过 client scope mapper 显式追加 clientId 到 audience 才能通过此校验，
         * 见 realm-precision.json 中 rbac-service client 的 audience mapper）。
         */
        @Bean
        public JwtDecoder jwtDecoder() {
            String issuerUri = String.format("%s/realms/%s",
                properties.getKeycloak().getServerUrl(),
                properties.getKeycloak().getRealm());

            org.springframework.security.oauth2.jwt.NimbusJwtDecoder decoder =
                org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                    .withIssuerLocation(issuerUri)
                    .build();

            OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud",
                aud -> aud != null && aud.contains(properties.getKeycloak().getClientId())
            );
            decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                issuerValidator, audienceValidator));

            return decoder;
        }

        /**
         * 只拦截 {@code gentry.oidc.protected-path-patterns} 匹配的路径（默认
         * {@code /api/sso/**}），其余路径继续走 gentry-core-spring-boot-starter
         * 的 {@code SecurityConfig}（Sa-Token 本地认证，permitAll 放行给 Sa-Token
         * 拦截器处理）。
         *
         * <p>Spring Security 允许多个 {@link SecurityFilterChain} bean 共存，条件
         * 是不能有两个以上使用无限定的 {@code anyRequest()}——这里显式收窄
         * {@code securityMatcher}，并用 {@code @Order} 保证本链先于 core 的兜底链
         * 被评估，避免 "multiple filter chains without securityMatcher" 冲突。</p>
         */
        @Bean
        public OidcJwtAuthenticationConverter oidcJwtAuthenticationConverter(OidcUserSyncService userSyncService) {
            return new OidcJwtAuthenticationConverter(userSyncService);
        }

        @Bean
        @org.springframework.core.annotation.Order(
            org.springframework.boot.autoconfigure.security.SecurityProperties.DEFAULT_FILTER_ORDER - 10)
        public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                                                  OidcJwtAuthenticationConverter converter) throws Exception {
            String[] patterns = properties.getProtectedPathPatterns();

            http
                .securityMatcher(patterns)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(converter)
                    )
                );

            return http.build();
        }
    }
}
