package com.gentry.rbac.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自助注册配置（前缀 {@code gentry.register}）。
 *
 * <p>平台不感知消费方业务角色：新注册用户要绑哪个角色由消费方以 roleCode 指定，
 * 角色不存在时静默跳过（不阻断注册，后台补绑即可）。</p>
 */
@ConfigurationProperties(prefix = "gentry.register")
public class GentryRegisterProperties {

    /**
     * 邮箱验证通过创建用户后自动绑定的角色编码；空 = 不绑定（默认）。
     * 例：budget 配 {@code gentry.register.default-role-code: BUDGET_USER}
     */
    private String defaultRoleCode = "";

    public String getDefaultRoleCode() { return defaultRoleCode; }
    public void setDefaultRoleCode(String defaultRoleCode) { this.defaultRoleCode = defaultRoleCode; }
}
