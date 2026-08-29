package com.gentry.core.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类。
 *
 * <p>所有方法都要求传 {@code defaultValue}，找不到 key 时原样返回它 ——
 * <b>界面绝不允许露出裸 key</b>。</p>
 */
@Component
public class I18nUtil {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final MessageSource messageSource;

    public I18nUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /** 隐式 locale（取 {@code LocaleContextHolder}），无参数 */
    public String getMessage(String key, String defaultValue) {
        return getMessage(key, defaultValue, EMPTY_ARGS);
    }

    /**
     * 隐式 locale + 参数化。用于「密码输入错误{0}次」这类模板。
     *
     * <p>注意 {@code MessageFormat} 把单引号当转义符：带 {@code {n}} 占位的译文里，
     * 撇号必须写成 {@code ''}，否则占位符会原样输出且不报错。
     * 由 {@code MessageFormatQuoteTest} 扫描保证。</p>
     */
    public String getMessage(String key, String defaultValue, Object... args) {
        return getMessage(key, LocaleContextHolder.getLocale(), defaultValue, args);
    }

    /**
     * 显式 locale。<b>无请求上下文的场景必须用这个重载</b>：定时通知、异步短信、
     * 给指定收件人的批量导出。
     *
     * <p>这类场景 {@code LocaleContextHolder} 是空的或属于别的线程
     * （{@code TraceUtils.wrap} 只传递 MDC，不传递 {@code UserContext} 与
     * {@code LocaleContextHolder}），必须由调用方从收件人的
     * {@code sys_user.language} 取值传入。</p>
     */
    public String getMessage(String key, Locale locale, String defaultValue, Object... args) {
        if (key == null) {
            return defaultValue;
        }
        try {
            return messageSource.getMessage(key, args, defaultValue, locale);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** 翻译操作日志类型（{@code operlog.type.INSERT} 等，7 个 key） */
    public String getOperLogTypeLabel(String type) {
        if (type == null) return null;
        return getMessage("operlog.type." + type, type);
    }
}
