package com.gentry.rbac.user.vo;

/**
 * 登录验证码响应 VO。
 * <p>{@code img} 为 data URI（{@code data:image/png;base64,...}），前端可直接用作 &lt;img src&gt;。</p>
 * <p>{@code uuid} 为本次验证码的唯一标识，登录时连同用户输入回传用于校验。</p>
 */
public class CaptchaVO {
    private String uuid;
    private String img;

    public CaptchaVO() {}

    public CaptchaVO(String uuid, String img) {
        this.uuid = uuid;
        this.img = img;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
}
