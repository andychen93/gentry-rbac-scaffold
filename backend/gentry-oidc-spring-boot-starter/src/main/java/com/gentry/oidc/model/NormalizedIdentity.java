package com.gentry.oidc.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 规范化身份对象。
 * <p>无论通过 Keycloak SSO 还是本地密码登录，最终都统一成此对象，
 * 供 RBAC 业务使用。</p>
 *
 * <p><b>关键原则</b>：</p>
 * <ul>
 *     <li>externalSubject: 外部身份源的唯一标识（如 Keycloak 的 sub）</li>
 *     <li>identityProvider: 身份源类型（如 "keycloak" 或 "local"）</li>
 *     <li>localUserId: 映射到本地用户的 ID（可能为 null，需要 JIT 同步）</li>
 *     <li>邮箱和用户名是资料，不能单独作为绑定依据</li>
 * </ul>
 */
@Data
@Builder
public class NormalizedIdentity {

    /** 外部身份源的唯一标识（如 Keycloak 的 sub） */
    private String externalSubject;

    /** 身份源类型（如 "keycloak" 或 "local"） */
    private String identityProvider;

    /** 本地用户 ID（可能为 null，需要 JIT 同步） */
    private Long localUserId;

    /** 用户名（资料字段） */
    private String username;

    /** 邮箱（资料字段） */
    private String email;

    /** 显示名（资料字段） */
    private String displayName;

    /** 手机号（资料字段） */
    private String phoneNumber;

    /** 角色列表 */
    private Set<String> roles;

    /** 部门 ID（数据权限） */
    private Long deptId;

    /** 语言偏好 */
    private String language;

    /** 头像 URL */
    private String avatar;

    /** 额外属性 */
    private Set<String> permissions;
}
