package com.precision.rbac.user.service.impl;

import com.precision.core.common.ErrorCode;
import com.precision.core.config.CaptchaProperties;
import com.precision.core.exception.BizException;
import com.precision.rbac.user.service.CaptchaService;
import com.precision.rbac.user.vo.CaptchaVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现：算术验证码（默认）或字符验证码，答案存 Redis，java.awt 绘图。
 *
 * <p>Spring Boot 默认 headless=true，服务端 AWT 绘图可用。</p>
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    /** 字符验证码取值池（去除易混 0/O/1/I） */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate redis;
    private final CaptchaProperties properties;
    private final java.util.Random random = new java.util.Random();

    public CaptchaServiceImpl(StringRedisTemplate redis, CaptchaProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public CaptchaVO generate() {
        String text;
        String answer;
        if ("math".equalsIgnoreCase(properties.getType())) {
            // 算术验证码：a+b 或 a-b（保证非负）
            int a = random.nextInt(10);
            int b = random.nextInt(10);
            if (random.nextInt(2) == 0) {
                text = a + "+" + b + "=?";
                answer = String.valueOf(a + b);
            } else {
                int big = Math.max(a, b), small = Math.min(a, b);
                text = big + "-" + small + "=?";
                answer = String.valueOf(big - small);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < properties.getLength(); i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            text = sb.toString();
            answer = text;
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(KEY_PREFIX + uuid, answer,
                properties.getExpireSeconds(), TimeUnit.SECONDS);
        return new CaptchaVO(uuid, drawImage(text));
    }

    @Override
    public void validate(String uuid, String input) {
        if (uuid == null || uuid.isBlank() || input == null || input.isBlank()) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
        String key = KEY_PREFIX + uuid;
        String stored = redis.opsForValue().get(key);
        // 一次性：无论对错都删除，防止重放
        redis.delete(key);
        if (stored == null || !stored.equalsIgnoreCase(input.trim())) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
    }

    private String drawImage(String text) {
        int w = properties.getWidth();
        int h = properties.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            // 干扰线
            g.setColor(new Color(230, 230, 230));
            for (int i = 0; i < 6; i++) {
                g.drawLine(random.nextInt(w), random.nextInt(h), random.nextInt(w), random.nextInt(h));
            }
            // 文本
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
            g.drawString(text, 8, 28);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "验证码生成失败");
        } finally {
            g.dispose();
        }
    }
}
