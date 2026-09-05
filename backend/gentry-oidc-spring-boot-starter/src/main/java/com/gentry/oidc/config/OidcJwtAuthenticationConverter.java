package com.gentry.oidc.config;

import com.gentry.oidc.model.NormalizedIdentity;
import com.gentry.oidc.service.OidcUserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 认证转换器。
 * <p>将已通过签名/issuer/exp/nbf 校验的 {@link Jwt} 转换为规范化身份对象，
 * 触发 JIT 用户同步，并映射为 Spring Security 的 {@link JwtAuthenticationToken}。</p>
 *
 * <p>签名、issuer、有效期校验已由上游 {@code JwtDecoder} 完成
 * （见 {@link OidcAutoConfiguration}），本类负责声明提取、角色映射和
 * 调用 {@link OidcUserSyncService} 完成首次登录的本地用户同步。</p>
 *
 * <p><b>同步失败不阻断认证</b>：{@code syncUser} 异常只记录日志，不影响
 * Spring Security 层的认证结果——JIT 同步是"锦上添花"，本地映射缺失时
 * 应用侧仍可基于 {@code NormalizedIdentity} 的角色做粗粒度授权判断，
 * 精确的本地用户 ID 关联可后续补偿。</p>
 */
@Slf4j
public class OidcJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final OidcUserSyncService userSyncService;

    public OidcJwtAuthenticationConverter(OidcUserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        NormalizedIdentity identity = buildNormalizedIdentity(jwt);

        try {
            Long localUserId = userSyncService.syncUser(identity);
            identity.setLocalUserId(localUserId);
        } catch (Exception e) {
            log.error("JIT user sync failed for externalSubject={}, proceeding with Security authentication only",
                identity.getExternalSubject(), e);
        }

        Collection<GrantedAuthority> authorities = buildAuthorities(identity);
        return new JwtAuthenticationToken(jwt, authorities, identity.getUsername());
    }

    /**
     * 从 JWT claims 构建规范化身份对象。
     */
    @SuppressWarnings("unchecked")
    private NormalizedIdentity buildNormalizedIdentity(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();

        String sub = jwt.getSubject();
        String email = (String) claims.get("email");
        String preferredUsername = (String) claims.get("preferred_username");
        String name = (String) claims.get("name");

        Set<String> roles = extractRealmRoles(claims);

        return NormalizedIdentity.builder()
            .externalSubject(sub)
            .identityProvider("keycloak")
            .username(preferredUsername)
            .email(email)
            .displayName(name)
            .roles(roles)
            .build();
    }

    /**
     * 提取 realm_access.roles 声明（Keycloak 标准角色映射器输出格式）。
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractRealmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map)) {
            return Collections.emptySet();
        }
        Object roles = ((Map<String, Object>) realmAccess).get("roles");
        if (!(roles instanceof List)) {
            return Collections.emptySet();
        }
        return new HashSet<>((List<String>) roles);
    }

    /**
     * 构建权限列表（ROLE_ 前缀，符合 Spring Security 默认命名约定）。
     */
    private Collection<GrantedAuthority> buildAuthorities(NormalizedIdentity identity) {
        if (identity.getRoles() == null || identity.getRoles().isEmpty()) {
            return Collections.emptyList();
        }
        return identity.getRoles().stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
}
