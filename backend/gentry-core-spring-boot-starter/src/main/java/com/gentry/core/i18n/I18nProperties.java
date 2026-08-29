package com.gentry.core.i18n;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 国际化配置。
 *
 * <pre>
 * gentry:
 *   i18n:
 *     enabled: true
 *     default-locale: zh_CN
 *     supported-locales: zh_CN,en_US
 * </pre>
 *
 * <p>{@code supported-locales} 是<b>语言白名单的唯一真源</b>。加一门语言时改这里 +
 * 补资源文件即可，不要在 DTO 的 {@code @Pattern} 里再写一份语言清单——注解常量读不到
 * 配置，两处维护必然漂移，漏改的表现是「配置里加了语言但接口拒绝保存」。</p>
 */
@Component
@ConfigurationProperties(prefix = "gentry.i18n")
public class I18nProperties {

    /**
     * 是否启用自定义语言解析。
     *
     * <p>关闭时不注册 {@code GentryLocaleResolver}，行为退回 Spring 默认的
     * {@code AcceptHeaderLocaleResolver}；{@code I18nUtil} 仍可用。派生项目若确定
     * 单语言交付，关掉即可，不必删代码。</p>
     */
    private boolean enabled = true;

    /** 默认语言。仅在用户偏好与请求头都给不出答案时兜底 */
    private String defaultLocale = "zh_CN";

    /** 支持语言白名单。加语言必须同步补资源文件，否则会全量走 defaultValue */
    private List<String> supportedLocales = new ArrayList<>(Arrays.asList("zh_CN", "en_US"));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDefaultLocale() { return defaultLocale; }
    public void setDefaultLocale(String defaultLocale) { this.defaultLocale = defaultLocale; }

    public List<String> getSupportedLocales() { return supportedLocales; }
    public void setSupportedLocales(List<String> supportedLocales) { this.supportedLocales = supportedLocales; }
}
