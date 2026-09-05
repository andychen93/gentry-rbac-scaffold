package com.gentry.oidc.service;

import com.gentry.oidc.model.NormalizedIdentity;

/**
 * 外部身份映射仓库 SPI。
 * <p>oidc-starter 不直接依赖 rbac-starter 的 {@code sys_user} /
 * {@code external_identity} 表结构，由消费方（gentry-start 或业务项目）
 * 实现此接口并注册为 Spring Bean，{@link OidcUserSyncService} 通过
 * {@link java.util.Optional} 注入，未提供实现时 JIT 同步会记录警告而非静默失败。</p>
 */
public interface ExternalIdentityRepository {

    /**
     * 根据 (identityProvider, externalSubject) 查找已绑定的本地用户 ID。
     *
     * @param identityProvider 身份源类型，如 "keycloak"
     * @param externalSubject  外部身份源的唯一标识（如 Keycloak 的 sub）
     * @return 本地用户 ID，未找到返回 {@code null}
     */
    Long findLocalUserId(String identityProvider, String externalSubject);

    /**
     * 创建本地用户并写入外部身份映射（JIT 首次登录）。
     *
     * @param identity    规范化身份对象
     * @param defaultRole 默认角色编码
     * @return 新创建的本地用户 ID
     */
    Long createUserWithIdentity(NormalizedIdentity identity, String defaultRole);
}
