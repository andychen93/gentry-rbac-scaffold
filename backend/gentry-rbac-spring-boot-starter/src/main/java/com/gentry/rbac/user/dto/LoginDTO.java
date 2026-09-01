package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginDTO {

    @NotBlank(message = "{valid.common.username.notBlank}")
    private String username;

    @NotBlank(message = "{valid.common.password.notBlank}")
    private String password;

    /** 验证码唯一标识（验证码开关开启时必填，由 CaptchaService 校验） */
    private String uuid;

    /** 验证码答案 */
    private String captcha;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
}
