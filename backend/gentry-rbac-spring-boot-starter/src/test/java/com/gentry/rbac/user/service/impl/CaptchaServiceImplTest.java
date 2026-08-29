package com.gentry.rbac.user.service.impl;

import com.gentry.core.config.CaptchaProperties;
import com.gentry.core.exception.BizException;
import com.gentry.rbac.config.SysConfigResolver;
import com.gentry.rbac.user.service.CaptchaService;
import com.gentry.rbac.user.vo.CaptchaVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证码服务单元测试：generate（画图+写 Redis）、validate（比对+一次性删除）。
 * 不启动 Spring；StringRedisTemplate 用 Mockito。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SysConfigResolver sysConfig;

    private CaptchaService service;

    @BeforeEach
    void setup() {
        service = new CaptchaServiceImpl(redis, new CaptchaProperties(), sysConfig); // 默认 math/300s
        // 默认场景：开关交给 yml 默认值（true），需要关的用例自行 stub
        lenient().when(sysConfig.getBoolean(eq(SysConfigResolver.KEY_CAPTCHA_ENABLED), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void generate_returnsBase64ImgAndUuid_andStoresAnswer() {
        when(redis.opsForValue()).thenReturn(valueOps);
        CaptchaVO vo = service.generate();
        assertThat(vo.getUuid()).isNotBlank();
        assertThat(vo.getImg()).startsWith("data:image/png;base64,");
        verify(valueOps).set(startsWith("captcha:"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void generate_disabledBySysConfig_returnsNullWithoutTouchingRedis() {
        // sys.captcha.enabled=false：不发图、不写 Redis（登录页据此不渲染验证码框）
        when(sysConfig.getBoolean(eq(SysConfigResolver.KEY_CAPTCHA_ENABLED), anyBoolean())).thenReturn(false);
        assertThat(service.generate()).isNull();
        verifyNoInteractions(redis);
    }

    @Test
    void validate_correctMatch_passesAndDeletes() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:abc")).thenReturn("7");
        service.validate("abc", "7"); // 不抛即通过
        verify(redis).delete("captcha:abc");
    }

    @Test
    void validate_wrongMatch_throwsAndStillDeletes() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:abc")).thenReturn("7");
        assertThatThrownBy(() -> service.validate("abc", "8"))
                .isInstanceOf(BizException.class);
        verify(redis).delete("captcha:abc"); // 一次性：错也删，防重放
    }

    @Test
    void validate_expired_throws() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:gone")).thenReturn(null);
        assertThatThrownBy(() -> service.validate("gone", "1"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void validate_blankInput_throwsWithoutRedis() {
        // uuid/captcha 空 → 直接抛，不查 Redis
        assertThatThrownBy(() -> service.validate("", "1"))
                .isInstanceOf(BizException.class);
    }
}
