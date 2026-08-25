package com.gentry.rbac.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentry.rbac.log.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@code LogAspect.sanitizeParams} 的回归测试。
 *
 * <p><b>它守的是一个 MockMvc 测不出来的缺陷。</b>操作日志切面要把方法参数序列化成 JSON 存库，
 * 而下载类接口的参数里有 {@code HttpServletResponse}；Jackson 序列化它时会去调 bean getter，
 * 其中包括 {@code getWriter()}，把 Catalina Response 的 {@code usingWriter} 置成 true。
 * 业务代码随后 {@code response.getOutputStream()} 就抛
 * 「getWriter() has already been called for this response」，用户导出在真实容器里必挂 500。</p>
 *
 * <p>为什么 MockMvc 测不出来：{@code MockHttpServletResponse} 允许先 {@code getWriter()}
 * 再 {@code getOutputStream()}，所以 {@code I18nApiIT} 里那几条导出用例一路绿灯，
 * 只有真机跑才暴露。所以这条测试**直接断言 getWriter 没被调过**，
 * 而不是去断言某个 HTTP 状态码。</p>
 */
class LogAspectSanitizeParamsTest {

    private LogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new LogAspect(Mockito.mock(LogService.class), new ObjectMapper());
    }

    /**
     * sanitizeParams 是私有方法，用原生反射调用 —— 它的行为就是本测试的主题。
     *
     * <p>不用 Spring 的 ReflectionTestUtils：gentry-business 的测试作用域里没有 spring-test。</p>
     */
    private Object[] sanitize(Object... args) {
        try {
            var m = LogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
            m.setAccessible(true);
            return (Object[]) m.invoke(aspect, (Object) args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("sanitizeParams 签名变了？", e);
        }
    }

    @Test
    @DisplayName("HttpServletResponse不交给Jackson_绝不触发getWriter")
    void HttpServletResponse不交给Jackson_绝不触发getWriter() throws Exception {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Object[] sanitized = sanitize(new Object(), response);

        // 这一行是整条测试的核心：碰一下 getWriter 就会让后续 getOutputStream 报废
        verify(response, never()).getWriter();
        verify(response, never()).getOutputStream();
        assertThat(sanitized[1]).asString()
                .as("应替换成类型占位符，保持参数位置可读")
                .startsWith("<")
                .endsWith(">");
    }

    @Test
    @DisplayName("HttpServletRequest与MultipartFile同样被替换")
    void HttpServletRequest与MultipartFile同样被替换() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        MultipartFile file = Mockito.mock(MultipartFile.class);

        Object[] sanitized = sanitize(request, file);

        assertThat(sanitized[0]).asString().startsWith("<").endsWith(">");
        assertThat(sanitized[1]).asString().startsWith("<").endsWith(">");
    }

    @Test
    @DisplayName("普通DTO原样保留_不影响既有日志内容")
    void 普通DTO原样保留_不影响既有日志内容() {
        Map<String, Object> dto = Map.of("username", "alice", "age", 30);

        Object[] sanitized = sanitize(dto);

        assertThat(sanitized[0]).isSameAs(dto);
    }

    @Test
    @DisplayName("含密码字段仍被脱敏_原有行为不回退")
    void 含密码字段仍被脱敏_原有行为不回退() {
        Map<String, Object> dto = Map.of("username", "alice", "password", "Abc@123456");

        Object[] sanitized = sanitize(dto);

        assertThat(sanitized[0]).asString()
                .as("password 必须被替换成 ******")
                .contains("******")
                .doesNotContain("Abc@123456");
    }

    @Test
    @DisplayName("null参数保持null_不越位")
    void null参数保持null_不越位() {
        Object[] sanitized = sanitize(null, "x");

        assertThat(sanitized[0]).isNull();
        assertThat(sanitized[1]).isEqualTo("x");
    }
}
