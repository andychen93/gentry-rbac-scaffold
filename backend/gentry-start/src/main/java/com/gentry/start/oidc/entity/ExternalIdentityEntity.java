package com.gentry.start.oidc.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部身份映射实体，映射 sys_external_identity 表。
 * <p>承载 gentry-oidc-spring-boot-starter 的 {@code ExternalIdentityRepository}
 * SPI 落库实现，见 {@link com.gentry.start.oidc.ExternalIdentityRepositoryImpl}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_external_identity")
public class ExternalIdentityEntity extends BaseEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /** 身份源类型，如 "keycloak" */
    private String identityProvider;

    /** 外部身份源的唯一标识（如 Keycloak 的 sub） */
    private String externalSubject;

    /** 本地用户 ID */
    private Long localUserId;

    /** 额外属性（JSON 字符串） */
    private String extraAttributes;
    // createBy, createTime, updateBy, updateTime, deleted 由 BaseEntity 基类提供
}
