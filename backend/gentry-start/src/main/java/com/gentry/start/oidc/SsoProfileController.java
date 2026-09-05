package com.gentry.start.oidc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SSO 保护接口冒烟测试端点。
 * <p>挂在 {@code /api/sso/**} 下，由 gentry-oidc-spring-boot-starter 的
 * {@code KeycloakSecurityConfig} 过滤器链保护（见 {@code gentry.oidc.protected-path-patterns}）。
 * 仅用于 M2 端到端验证，不属于正式业务接口。</p>
 */
@Slf4j
@RestController
public class SsoProfileController {

    @GetMapping("/api/sso/profile")
    public Map<String, Object> profile(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        log.info("SSO profile accessed: subject={}, username={}", jwt.getSubject(), authentication.getName());
        return Map.of(
            "subject", jwt.getSubject(),
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities().toString(),
            "issuer", jwt.getIssuer().toString()
        );
    }
}
