package com.gentry.oidc.service;

import com.gentry.oidc.event.OidcAuthEvent;
import com.gentry.oidc.model.NormalizedIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcUserSyncServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ExternalIdentityRepository repository;

    @Test
    void syncUser_repositoryMissing_publishesFailureAndReturnsNull() {
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, Optional.empty());
        NormalizedIdentity identity = identity("sub-1");

        Long result = service.syncUser(identity);

        assertThat(result).isNull();
        ArgumentCaptor<OidcAuthEvent> captor = ArgumentCaptor.forClass(OidcAuthEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(OidcAuthEvent.Type.LOGIN_FAILURE);
    }

    @Test
    void syncUser_existingUser_publishesLoginSuccess() {
        when(repository.findLocalUserId("keycloak", "sub-2")).thenReturn(42L);
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, Optional.of(repository));
        NormalizedIdentity identity = identity("sub-2");

        Long result = service.syncUser(identity);

        assertThat(result).isEqualTo(42L);
        ArgumentCaptor<OidcAuthEvent> captor = ArgumentCaptor.forClass(OidcAuthEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(OidcAuthEvent.Type.LOGIN_SUCCESS);
    }

    @Test
    void syncUser_newUserAutoCreateEnabled_createsAndPublishesJitEvent() {
        when(repository.findLocalUserId("keycloak", "sub-3")).thenReturn(null);
        when(repository.createUserWithIdentity(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("USER")))
            .thenReturn(99L);
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, Optional.of(repository));
        service.setAutoCreateUser(true);
        service.setDefaultRole("USER");
        NormalizedIdentity identity = identity("sub-3");

        Long result = service.syncUser(identity);

        assertThat(result).isEqualTo(99L);
        ArgumentCaptor<OidcAuthEvent> captor = ArgumentCaptor.forClass(OidcAuthEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(OidcAuthEvent.Type.JIT_USER_CREATED);
    }

    @Test
    void syncUser_newUserAutoCreateDisabled_returnsNullAndPublishesFailure() {
        when(repository.findLocalUserId("keycloak", "sub-4")).thenReturn(null);
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, Optional.of(repository));
        service.setAutoCreateUser(false);
        NormalizedIdentity identity = identity("sub-4");

        Long result = service.syncUser(identity);

        assertThat(result).isNull();
        ArgumentCaptor<OidcAuthEvent> captor = ArgumentCaptor.forClass(OidcAuthEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(OidcAuthEvent.Type.LOGIN_FAILURE);
    }

    @Test
    void syncUser_nullIdentity_throwsIllegalArgumentException() {
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, Optional.of(repository));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.syncUser(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handleLogout_publishesLogoutEvent() {
        OidcUserSyncService service = new OidcUserSyncService(eventPublisher, Optional.of(repository));
        NormalizedIdentity identity = identity("sub-5");

        service.handleLogout(identity);

        ArgumentCaptor<OidcAuthEvent> captor = ArgumentCaptor.forClass(OidcAuthEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(OidcAuthEvent.Type.LOGOUT);
    }

    private NormalizedIdentity identity(String sub) {
        return NormalizedIdentity.builder()
            .externalSubject(sub)
            .identityProvider("keycloak")
            .username("user-" + sub)
            .build();
    }
}
