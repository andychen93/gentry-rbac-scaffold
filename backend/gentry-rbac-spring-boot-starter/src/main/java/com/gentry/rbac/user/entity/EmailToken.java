package com.gentry.rbac.user.entity;

import com.gentry.core.entity.TenantEntity;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 邮箱认证令牌：注册验证（REGISTER）与密码找回（RESET_PASSWORD）共用，
 * 一次性，只存 SHA-256。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_email_token")
public class EmailToken extends TenantEntity {

    public static final String PURPOSE_REGISTER = "REGISTER";
    public static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    /** 目标邮箱（小写规范化后存储） */
    private String email;
    /** REGISTER / RESET_PASSWORD */
    private String purpose;
    /** 令牌 SHA-256，不存明文 */
    private String tokenHash;
    /** 注册凭据 JSON（bcrypt 密码哈希 + 昵称）；RESET 用途为空 */
    private String payload;
    /** REGISTER 验证通过后回填创建的用户 id */
    private Long userId;
    private LocalDateTime expiresAt;
    /** 一次性：用后回填 */
    private LocalDateTime usedAt;
    // tenantId, createBy, createTime, updateBy, updateTime, deleted 由 TenantEntity 基类提供
}
