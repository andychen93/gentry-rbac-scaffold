package com.gentry.oidc.config;

import com.gentry.oidc.service.OidcUserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcJwtAuthenticationConverterTest {

    @Mock
    private OidcUserSyncService userSyncService;

    private OidcJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OidcJwtAuthenticationConverter(userSyncService);
    }

    @Test
    void convert_withRealmRoles_mapsToRoleAuthorities() {
        Jwt jwt = buildJwt(Map.of(
            "sub", "kc-subject-123",
            "email", "user@example.com",
            "preferred_username", "alice",
            "name", "Alice",
            "realm_access", Map.of("roles", List.of("admin", "user"))
        ));
        when(userSyncService.syncUser(any())).thenReturn(1L);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("alice");
        assertThat(token.getAuthorities())
            .extracting(Object::toString)
            .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void convert_withoutRealmAccess_producesNoAuthorities() {
        Jwt jwt = buildJwt(Map.of(
            "sub", "kc-subject-456",
            "preferred_username", "bob"
        ));
        when(userSyncService.syncUser(any())).thenReturn(2L);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.getName()).isEqualTo("bob");
    }

    @Test
    void convert_missingRolesList_producesNoAuthorities() {
        Jwt jwt = buildJwt(Map.of(
            "sub", "kc-subject-789",
            "preferred_username", "carol",
            "realm_access", Map.of("notRoles", "x")
        ));
        when(userSyncService.syncUser(any())).thenReturn(3L);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_callsUserSyncService() {
        Jwt jwt = buildJwt(Map.of(
            "sub", "kc-subject-999",
            "preferred_username", "dave"
        ));
        when(userSyncService.syncUser(any())).thenReturn(42L);

        converter.convert(jwt);

        verify(userSyncService).syncUser(any());
    }

    @Test
    void convert_syncUserThrows_stillReturnsAuthenticationToken() {
        Jwt jwt = buildJwt(Map.of(
            "sub", "kc-subject-error",
            "preferred_username", "erin"
        ));
        when(userSyncService.syncUser(any())).thenThrow(new RuntimeException("db down"));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("erin");
    }

    private Jwt buildJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claims(c -> c.putAll(claims))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }
}
