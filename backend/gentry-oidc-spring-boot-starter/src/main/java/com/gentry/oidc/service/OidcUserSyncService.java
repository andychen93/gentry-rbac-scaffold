package com.gentry.oidc.service;

import com.gentry.oidc.event.OidcAuthEvent;
import com.gentry.oidc.model.NormalizedIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * OIDC 用户同步服务（JIT - Just-In-Time）。
 * <p>首次通过 Keycloak 登录时，自动创建本地用户记录。</p>
 *
 * <p><b>持久化留白说明</b>：本 starter 不直接依赖 rbac-starter 的
 * {@code sys_user} / {@code external_identity} 表结构（避免模块间硬耦合）。
 * {@link #findLocalUserId} 与 {@link #createUser} 通过 {@link ExternalIdentityRepository}
 * SPI 接口留白，由消费方（gentry-start 或业务项目）提供实现 Bean；
 * 未提供实现时，JIT 同步会记录警告日志并返回 {@code null}，不会静默失败。</p>
 */
@Slf4j
@Service
public class OidcUserSyncService {

    /** 自动创建用户开关，对应 gentry.oidc.auto-create-user */
    private boolean autoCreateUser = true;

    /** 默认角色，对应 gentry.oidc.default-role */
    private String defaultRole = "USER";

    private final ApplicationEventPublisher eventPublisher;
    private final ExternalIdentityRepository repository;

    @Autowired
    public OidcUserSyncService(ApplicationEventPublisher eventPublisher,
                                 java.util.Optional<ExternalIdentityRepository> repository) {
        this.eventPublisher = eventPublisher;
        this.repository = repository.orElse(null);
    }

    public void setAutoCreateUser(boolean autoCreateUser) {
        this.autoCreateUser = autoCreateUser;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    /**
     * JIT 同步用户。
     * <p>如果用户不存在且 autoCreateUser=true，则创建本地用户。</p>
     *
     * @param identity 规范化身份对象
     * @return 同步后的用户 ID，仓库未配置或创建失败时返回 {@code null}
     */
    public Long syncUser(NormalizedIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Identity cannot be null");
        }
        if (repository == null) {
            log.warn("ExternalIdentityRepository bean not configured; skip JIT sync for externalSubject={}",
                identity.getExternalSubject());
            eventPublisher.publishEvent(new OidcAuthEvent(
                OidcAuthEvent.Type.LOGIN_FAILURE, identity, "ExternalIdentityRepository not configured"));
            return null;
        }

        Long existingUserId = repository.findLocalUserId(identity.getIdentityProvider(), identity.getExternalSubject());

        if (existingUserId != null) {
            log.debug("User already exists: externalSubject={}, localUserId={}",
                identity.getExternalSubject(), existingUserId);
            eventPublisher.publishEvent(new OidcAuthEvent(
                OidcAuthEvent.Type.LOGIN_SUCCESS, identity, null));
            return existingUserId;
        }

        if (!autoCreateUser) {
            log.warn("User not found and auto-create disabled: externalSubject={}, provider={}",
                identity.getExternalSubject(), identity.getIdentityProvider());
            eventPublisher.publishEvent(new OidcAuthEvent(
                OidcAuthEvent.Type.LOGIN_FAILURE, identity, "auto-create disabled"));
            return null;
        }

        Long newUserId = repository.createUserWithIdentity(identity, defaultRole);
        log.info("User auto-created via JIT: externalSubject={}, localUserId={}, username={}",
            identity.getExternalSubject(), newUserId, identity.getUsername());
        eventPublisher.publishEvent(new OidcAuthEvent(
            OidcAuthEvent.Type.JIT_USER_CREATED, identity, "role=" + defaultRole));

        return newUserId;
    }

    /**
     * 处理登出：发布登出审计事件。
     * <p>Token 黑名单写入由消费方在调用登出接口时通过
     * {@code gentry-core-spring-boot-starter} 的 {@code TokenBlacklistService} 完成，
     * 本方法只负责审计事件发布。</p>
     */
    public void handleLogout(NormalizedIdentity identity) {
        eventPublisher.publishEvent(new OidcAuthEvent(OidcAuthEvent.Type.LOGOUT, identity, null));
    }
}
