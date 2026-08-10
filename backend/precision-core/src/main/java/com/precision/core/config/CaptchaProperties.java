package com.precision.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录验证码配置。
 *
 * <pre>
 * precision:
 *   captcha:
 *     enabled: true
 *     type: math        # math=算术 | char=字符
 *     length: 4
 *     expire-seconds: 300
 *     width: 120
 *     height: 40
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "precision.captcha")
public class CaptchaProperties {

    /** 是否启用验证码（关闭时登录不校验验证码） */
    private boolean enabled = true;

    /** 验证码类型：math=算术验证码，char=字符验证码 */
    private String type = "math";

    /** 字符长度（算术验证码为运算数位数） */
    private int length = 4;

    /** 验证码有效期（秒） */
    private int expireSeconds = 300;

    /** 图片宽度（像素） */
    private int width = 120;

    /** 图片高度（像素） */
    private int height = 40;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }

    public int getExpireSeconds() { return expireSeconds; }
    public void setExpireSeconds(int expireSeconds) { this.expireSeconds = expireSeconds; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
}
