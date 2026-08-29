package com.gentry.core.i18n;

import com.gentry.core.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 语言解析器测试。
 *
 * <p>覆盖三级链的每个分支（对应详细设计 U-01 ~ U-08），以及 Accept-Language 的
 * 精确匹配 / 语言退化 / 畸形头三种情况。</p>
 */
class GentryLocaleResolverTest {

    private static final Locale ZH_CN = Locale.of("zh", "CN");
    private static final Locale EN_US = Locale.of("en", "US");

    private GentryLocaleResolver resolver;

    @BeforeEach
    void setUp() {
        I18nProperties props = new I18nProperties();
        props.setDefaultLocale("zh_CN");
        props.setSupportedLocales(Arrays.asList("zh_CN", "en_US"));
        resolver = new GentryLocaleResolver(props);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private MockHttpServletRequest requestWithHeader(String acceptLanguage) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (acceptLanguage != null) {
            req.addHeader("Accept-Language", acceptLanguage);
        }
        return req;
    }

    // ==================== ① 用户偏好 ====================

    @Test
    @DisplayName("resolve_用户偏好为enUS_返回enUS")
    void resolve_用户偏好为enUS_返回enUS() {
        assertThat(resolver.resolve("en_US", requestWithHeader("zh-CN"))).isEqualTo(EN_US);
    }

    @Test
    @DisplayName("resolve_用户偏好与请求头冲突_用户偏好优先")
    void resolve_用户偏好与请求头冲突_用户偏好优先() {
        assertThat(resolver.resolve("zh_CN", requestWithHeader("en-US,en;q=0.9"))).isEqualTo(ZH_CN);
    }

    @Test
    @DisplayName("resolve_用户偏好为未支持语言_降级到请求头")
    void resolve_用户偏好为未支持语言_降级到请求头() {
        assertThat(resolver.resolve("ja_JP", requestWithHeader("en-US"))).isEqualTo(EN_US);
    }

    @Test
    @DisplayName("resolve_用户偏好为非法串_降级到请求头")
    void resolve_用户偏好为非法串_降级到请求头() {
        assertThat(resolver.resolve("!!!", requestWithHeader("en-US"))).isEqualTo(EN_US);
    }

    // ==================== ② 请求头 ====================

    @Test
    @DisplayName("resolve_用户偏好为null但请求头为enUS_返回enUS")
    void resolve_用户偏好为null但请求头为enUS_返回enUS() {
        // sys_user.language IS NULL 的三态语义：跟随浏览器
        assertThat(resolver.resolve(null, requestWithHeader("en-US"))).isEqualTo(EN_US);
    }

    @Test
    @DisplayName("resolve_请求头只有语言码无地区_退化匹配到enUS")
    void resolve_请求头只有语言码无地区_退化匹配到enUS() {
        // 浏览器可能只发 "en"，只做精确匹配会落空
        assertThat(resolver.resolve(null, requestWithHeader("en"))).isEqualTo(EN_US);
    }

    @Test
    @DisplayName("resolve_请求头为zhTW_退化命中zhCN")
    void resolve_请求头为zhTW_退化命中zhCN() {
        // 有意为之：繁体用户看简体优于看英文
        assertThat(resolver.resolve(null, requestWithHeader("zh-TW"))).isEqualTo(ZH_CN);
    }

    @Test
    @DisplayName("resolve_请求头带q权重_按权重取受支持的第一个")
    void resolve_请求头带q权重_按权重取受支持的第一个() {
        // ja 权重最高但不支持，应跳过取 en-US
        assertThat(resolver.resolve(null, requestWithHeader("ja;q=0.9,en-US;q=0.8")))
                .isEqualTo(EN_US);
    }

    @Test
    @DisplayName("resolve_请求头全部不受支持_返回默认")
    void resolve_请求头全部不受支持_返回默认() {
        assertThat(resolver.resolve(null, requestWithHeader("ja-JP,ko-KR"))).isEqualTo(ZH_CN);
    }

    @Test
    @DisplayName("resolve_请求头畸形_不抛异常并返回默认")
    void resolve_请求头畸形_不抛异常并返回默认() {
        assertThat(resolver.resolve(null, requestWithHeader("=====;;q=abc"))).isEqualTo(ZH_CN);
    }

    // ==================== ③ 兜底 ====================

    @Test
    @DisplayName("resolve_全部缺失_返回zhCN")
    void resolve_全部缺失_返回zhCN() {
        assertThat(resolver.resolve(null, requestWithHeader(null))).isEqualTo(ZH_CN);
    }

    @Test
    @DisplayName("resolve_request为null_返回默认不抛异常")
    void resolve_request为null_返回默认不抛异常() {
        // 无请求上下文的场景（定时任务）会走到这里
        assertThat(resolver.resolve(null, null)).isEqualTo(ZH_CN);
    }

    // ==================== resolveLocale / setLocale ====================

    @Test
    @DisplayName("resolveLocale_从UserContext取语言_与resolve一致")
    void resolveLocale_从UserContext取语言_与resolve一致() {
        UserContext.setLanguage("en_US");
        assertThat(resolver.resolveLocale(requestWithHeader("zh-CN"))).isEqualTo(EN_US);
    }

    @Test
    @DisplayName("setLocale_调用_抛UnsupportedOperationException")
    void setLocale_调用_抛UnsupportedOperationException() {
        assertThatThrownBy(() -> resolver.setLocale(requestWithHeader(null), null, EN_US))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ==================== parse ====================

    @Test
    @DisplayName("parse_下划线与连字符两种写法_均可解析")
    void parse_下划线与连字符两种写法_均可解析() {
        assertThat(GentryLocaleResolver.parse("zh_CN")).contains(ZH_CN);
        assertThat(GentryLocaleResolver.parse("zh-CN")).contains(ZH_CN);
        assertThat(GentryLocaleResolver.parse("ZH-cn")).contains(ZH_CN);
        assertThat(GentryLocaleResolver.parse("  en_US  ")).contains(EN_US);
    }

    @Test
    @DisplayName("parse_空白或空串_返回empty")
    void parse_空白或空串_返回empty() {
        assertThat(GentryLocaleResolver.parse(null)).isEmpty();
        assertThat(GentryLocaleResolver.parse("")).isEmpty();
        assertThat(GentryLocaleResolver.parse("   ")).isEmpty();
    }

    // ==================== 白名单构造 ====================

    @Test
    @DisplayName("构造_白名单全非法_至少保留默认语言")
    void 构造_白名单全非法_至少保留默认语言() {
        I18nProperties props = new I18nProperties();
        props.setDefaultLocale("zh_CN");
        props.setSupportedLocales(List.of("", "   "));
        GentryLocaleResolver r = new GentryLocaleResolver(props);
        assertThat(r.getSupported()).containsExactly(ZH_CN);
        assertThat(r.resolve("en_US", requestWithHeader("en-US"))).isEqualTo(ZH_CN);
    }

    @Test
    @DisplayName("构造_默认语言非法_退回简体中文")
    void 构造_默认语言非法_退回简体中文() {
        I18nProperties props = new I18nProperties();
        props.setDefaultLocale("");
        props.setSupportedLocales(List.of("zh_CN", "en_US"));
        assertThat(new GentryLocaleResolver(props).getDefaultLocale())
                .isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }
}
