package com.gentry.oidc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OIDC 配置属性。
 */
@Data
@ConfigurationProperties(prefix = "gentry.oidc")
public class OidcProperties {

    /** OIDC 模式：keycloak | local */
    private String mode = "keycloak";

    /**
     * 是否启用 OIDC（SSO 总开关）。
     * <p>默认 {@code false}：本 starter 与 rbac-starter 的 Sa-Token 本地认证并存于
     * classpath 时，若不显式开启不能影响现有系统的默认行为（架构文档「SSO 开关」
     * 约束：SSO 关闭后仍可本地授权鉴权）。消费方需在 application.yml 显式设置
     * {@code gentry.oidc.enabled=true} 才会装配 Keycloak Resource Server 过滤器链。</p>
     */
    private boolean enabled = false;

    /** 首次登录时是否自动创建本地用户（JIT） */
    private boolean autoCreateUser = true;

    /** JIT 创建用户时赋予的默认角色编码 */
    private String defaultRole = "USER";

    /**
     * Keycloak Resource Server 过滤器链拦截的路径模式。
     * <p>默认仅拦截 {@code /api/sso/**}，与 gentry-core-spring-boot-starter 的
     * Sa-Token 全局过滤器链（拦截其余路径）并存，不产生冲突。消费方需将需要
     * SSO 保护的 Controller 挂在此路径前缀下。</p>
     */
    private String[] protectedPathPatterns = {"/api/sso/**"};

    /** Keycloak 配置 */
    private KeycloakConfig keycloak = new KeycloakConfig();

    /** 本地模式配置 */
    private LocalConfig local = new LocalConfig();

    /**
     * Keycloak 配置。
     */
    @Data
    public static class KeycloakConfig {
        /** Keycloak 服务器地址 */
        private String serverUrl;

        /** Realm 名称 */
        private String realm;

        /** 客户端 ID */
        private String clientId;

        /** 客户端密钥 */
        private String clientSecret;
    }

    /**
     * 本地模式配置。
     */
    @Data
    public static class LocalConfig {
        /** JWT 密钥 */
        private String jwtSecretKey;

        /** JWT 过期时间（秒） */
        private Long expiration = 86400L;
    }
}
