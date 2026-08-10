package com.precision.rbac.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginDTO {

    @Size(max = 50, message = "租户编码最长50字符")
    private String tenantCode;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码唯一标识（验证码开关开启时必填，由 CaptchaService 校验） */
    private String uuid;

    /** 验证码答案 */
    private String captcha;

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
}
