package com.gentry.rbac.user.service;

import com.gentry.core.config.LoginSecurityProperties;
import com.gentry.rbac.config.SysConfigResolver;
import com.gentry.rbac.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录失败计数器单元测试：incr+TTL、锁定阈值、清除。
 * 计数基于 Redis（多节点一致），这里 mock StringRedisTemplate。
 */
@ExtendWith(MockitoExtension.class)
class LoginFailCounterServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ConfigService configService;

    private LoginFailCounterService service;

    @BeforeEach
    void setup() {
        // configService 未打桩 → getConfigValue 返回 null → 回退 yml 默认值（maxFail=5, lockMin=10）
        service = new LoginFailCounterService(
                redis, new LoginSecurityProperties(), new SysConfigResolver(configService, true));
    }

    @Test
    void recordFail_firstCall_setsLockWindowTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login_fail:1:u")).thenReturn(1L);
        service.recordFail(1L, "u");
        verify(redis).expire("login_fail:1:u", 10L, TimeUnit.MINUTES);
    }

    @Test
    void recordFail_subsequentCall_doesNotResetTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("login_fail:1:u")).thenReturn(3L);
        service.recordFail(1L, "u");
        verify(redis, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    void isLocked_belowMax_false() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_fail:1:u")).thenReturn("4");
        assertThat(service.isLocked(1L, "u")).isFalse();
    }

    @Test
    void isLocked_atMax_true() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_fail:1:u")).thenReturn("5");
        assertThat(service.isLocked(1L, "u")).isTrue();
    }

    @Test
    void isLocked_noRecord_false() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_fail:1:u")).thenReturn(null);
        assertThat(service.isLocked(1L, "u")).isFalse();
    }

    @Test
    void clear_deletesKey() {
        service.clear(1L, "u");
        verify(redis).delete("login_fail:1:u");
    }
}
