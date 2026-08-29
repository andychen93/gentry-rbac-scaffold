package com.gentry.core.i18n;

import com.gentry.core.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 语言解析器。三级链：① {@code sys_user.language} → ② {@code Accept-Language} → ③ 默认。
 *
 * <p><b>本类不是语言的设置点。</b>已登录请求的语言由 {@code SaTokenConfig} 的
 * UserContext 拦截器显式调用 {@link #resolve} + {@code LocaleContextHolder.setLocale}
 * 完成；本类作为 {@code LocaleResolver} bean 只服务未经过 {@code /api/**} 拦截器的
 * 请求（actuator、静态资源）。</p>
 *
 * <p><b>禁止改成实现 {@code LocaleContextResolver}。</b>{@code DispatcherServlet.buildLocaleContext}
 * 对 {@code LocaleResolver} 返回懒求值的 lambda、对 {@code LocaleContextResolver} 则在
 * {@code doService} 之前饿求值——后者会在拦截器写 {@code UserContext} 之前就把语言定稿，
 * 导致 {@code sys_user.language} <b>静默失效</b>。要加时区支持请另开组件。</p>
 */
public class GentryLocaleResolver implements LocaleResolver {

    private final Locale defaultLocale;
    /** 用 LinkedHashSet 保序：matchHeader 的退化匹配按配置顺序取第一个同语言项 */
    private final Set<Locale> supported;

    public GentryLocaleResolver(I18nProperties props) {
        this.defaultLocale = parse(props.getDefaultLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
        Set<Locale> set = new LinkedHashSet<>();
        for (String raw : props.getSupportedLocales()) {
            parse(raw).ifPresent(set::add);
        }
        if (set.isEmpty()) {
            set.add(this.defaultLocale);
        }
        this.supported = java.util.Collections.unmodifiableSet(set);
    }

    /**
     * 三级解析。<b>唯一的解析实现</b>，由 SaTokenConfig 拦截器与 {@link #resolveLocale}
     * 共用，保证「已登录请求」与「未登录 / 非 /api 请求」两条路径行为一致。
     *
     * @param userLanguage 用户偏好（{@code sys_user.language}）；未登录或未设置传 null
     * @param request      当前请求，用于读 {@code Accept-Language}；可为 null
     */
    public Locale resolve(String userLanguage, HttpServletRequest request) {
        // ① 用户级偏好（已登录且显式选择过时权威），白名单过滤
        Locale fromUser = parse(userLanguage).orElse(null);
        if (fromUser != null && supported.contains(fromUser)) {
            return fromUser;
        }
        // ② 请求头：登录前，或已登录但 sys_user.language IS NULL（从未选择过）
        if (request != null) {
            Locale fromHeader = matchHeader(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
            if (fromHeader != null) {
                return fromHeader;
            }
        }
        // ③ 兜底
        return defaultLocale;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        return resolve(UserContext.getLanguage(), request);
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException(
                "语言偏好由 sys_user.language 持久化，不支持通过 LocaleResolver 临时设置");
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public Set<Locale> getSupported() {
        return supported;
    }

    /**
     * 解析语言串，{@code zh_CN} / {@code zh-CN} / {@code en} / {@code ZH-cn} 均可。
     *
     * <p>只做格式解析，<b>不判断是否受支持</b>——白名单过滤由调用方做。非法值返回
     * {@link Optional#empty()} 而不抛异常：语言解析在请求最外层，抛异常会让整个请求 500，
     * 而语言错误应当降级而非中断。</p>
     */
    static Optional<Locale> parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String[] parts = raw.trim().replace('-', '_').split("_");
        try {
            Locale locale = switch (parts.length) {
                case 1 -> Locale.of(parts[0].toLowerCase(Locale.ROOT));
                case 2 -> Locale.of(parts[0].toLowerCase(Locale.ROOT), parts[1].toUpperCase(Locale.ROOT));
                default -> Locale.of(parts[0].toLowerCase(Locale.ROOT),
                        parts[1].toUpperCase(Locale.ROOT), parts[2]);
            };
            return locale.getLanguage().isEmpty() ? Optional.empty() : Optional.of(locale);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * 按 {@code Accept-Language} 的 q 权重降序，取第一个受支持的语言。
     *
     * <p>两级匹配：先精确（{@code en_US} == {@code en_US}），再按语言退化
     * （{@code en} → {@code en_US}）。退化是必要的：浏览器可能只发 {@code Accept-Language: en}，
     * 只做精确匹配会全部落空。反向地 {@code zh-TW} 在仅支持 {@code zh_CN} 时也会命中
     * {@code zh_CN}——这是有意的，繁体用户看简体比看英文合理。</p>
     *
     * @return 匹配到的受支持语言；无匹配或头畸形返回 null
     */
    private Locale matchHeader(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = Locale.LanguageRange.parse(header);   // 已按 q 降序
        } catch (IllegalArgumentException e) {
            return null;   // 畸形头：交由调用方退默认
        }
        for (Locale.LanguageRange range : ranges) {
            Locale candidate = parse(range.getRange()).orElse(null);
            if (candidate == null) {
                continue;
            }
            for (Locale s : supported) {
                if (s.equals(candidate)) {
                    return s;
                }
            }
            for (Locale s : supported) {
                if (s.getLanguage().equals(candidate.getLanguage())) {
                    return s;
                }
            }
        }
        return null;
    }
}
