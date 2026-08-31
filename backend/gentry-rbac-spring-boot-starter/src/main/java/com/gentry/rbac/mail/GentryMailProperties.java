package com.gentry.rbac.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮箱认证配置（前缀 {@code gentry.mail}）。
 *
 * <p>SMTP 连接参数不在此重复定义，直接复用 Spring 标准的 {@code spring.mail.host /
 * port / username / password}；这里只管业务语义：发件人、前端链接基址、令牌 TTL、
 * 重发冷却。</p>
 */
@ConfigurationProperties(prefix = "gentry.mail")
public class GentryMailProperties {

    /** 是否启用 SMTP 网关；false（默认）时用日志桩 */
    private boolean enabled = false;

    /** 发件人地址，enabled=true 时必填 */
    private String from = "no-reply@gentry.local";

    /** 验证邮件里链接的前端基址，如 https://budget.example.com */
    private String verifyUrlBase = "http://localhost:5173";

    /** 重置密码邮件里链接的前端基址 */
    private String resetUrlBase = "http://localhost:5173";

    /** 注册验证令牌有效期（小时） */
    private int registerTokenTtlHours = 48;

    /** 密码重置令牌有效期（分钟） */
    private int resetTokenTtlMinutes = 30;

    /** 同邮箱重发冷却（分钟），防刷 */
    private int resendCooldownMinutes = 1;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getVerifyUrlBase() { return verifyUrlBase; }
    public void setVerifyUrlBase(String verifyUrlBase) { this.verifyUrlBase = verifyUrlBase; }
    public String getResetUrlBase() { return resetUrlBase; }
    public void setResetUrlBase(String resetUrlBase) { this.resetUrlBase = resetUrlBase; }
    public int getRegisterTokenTtlHours() { return registerTokenTtlHours; }
    public void setRegisterTokenTtlHours(int registerTokenTtlHours) { this.registerTokenTtlHours = registerTokenTtlHours; }
    public int getResetTokenTtlMinutes() { return resetTokenTtlMinutes; }
    public void setResetTokenTtlMinutes(int resetTokenTtlMinutes) { this.resetTokenTtlMinutes = resetTokenTtlMinutes; }
    public int getResendCooldownMinutes() { return resendCooldownMinutes; }
    public void setResendCooldownMinutes(int resendCooldownMinutes) { this.resendCooldownMinutes = resendCooldownMinutes; }
}
