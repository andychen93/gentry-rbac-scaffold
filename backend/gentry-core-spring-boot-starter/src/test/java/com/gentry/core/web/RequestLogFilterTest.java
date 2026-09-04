package com.gentry.core.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("请求日志过滤器")
class RequestLogFilterTest {

    private RequestLogFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLogFilter(new RequestLogProperties());
    }

    @Test
    @DisplayName("默认排除路径命中 /actuator/**")
    void shouldSkip_actuator() {
        assertThat(filter.shouldSkip("/actuator/prometheus")).isTrue();
        assertThat(filter.shouldSkip("/actuator/health")).isTrue();
        assertThat(filter.shouldSkip("/api/v1/users")).isFalse();
    }

    @Test
    @DisplayName("不在排除列表的路径不跳过")
    void shouldSkip_regularApi() {
        assertThat(filter.shouldSkip("/api/v1/auth/login")).isFalse();
    }

    @Test
    @DisplayName("登录接口的 password 字段脱敏")
    void maskLoginPassword() {
        String body = "{\"username\":\"admin\",\"password\":\"Abc@123456\"}";
        String masked = filter.maskSensitiveBody("/api/v1/auth/login", body);
        assertThat(masked).contains("\"password\":\"***\"");
        assertThat(masked).doesNotContain("Abc@123456");
        assertThat(masked).contains("\"username\":\"admin\"");
    }

    @Test
    @DisplayName("修改密码接口的 oldPassword/newPassword 字段脱敏")
    void maskPasswordChange() {
        String body = "{\"oldPassword\":\"old123\",\"newPassword\":\"new456\"}";
        String masked = filter.maskSensitiveBody("/api/v1/auth/password", body);
        assertThat(masked).contains("\"oldPassword\":\"***\"").contains("\"newPassword\":\"***\"");
        assertThat(masked).doesNotContain("old123").doesNotContain("new456");
    }

    @Test
    @DisplayName("非敏感路径不修改 body")
    void nonSensitivePath_bodyUnchanged() {
        String body = "{\"username\":\"admin\",\"password\":\"x\"}";
        String masked = filter.maskSensitiveBody("/api/v1/users", body);
        assertThat(masked).isEqualTo(body);
    }

    @Test
    @DisplayName("truncate：短字符串原样返回")
    void truncate_short() {
        assertThat(RequestLogFilter.truncate("hello", 10)).isEqualTo("hello");
    }

    @Test
    @DisplayName("truncate：超长字符串截断并附加总长度")
    void truncate_long() {
        String s = "a".repeat(100);
        String out = RequestLogFilter.truncate(s, 10);
        assertThat(out).startsWith("aaaaaaaaaa").contains("100");
    }

    @Test
    @DisplayName("truncate：null 返回空字符串")
    void truncate_null() {
        assertThat(RequestLogFilter.truncate(null, 10)).isEmpty();
    }
}
