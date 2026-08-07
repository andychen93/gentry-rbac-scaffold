package com.precision.core.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化工具类
 */
@Component
public class I18nUtil {

    private final MessageSource messageSource;

    public I18nUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key, String defaultValue) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(key, null, defaultValue, locale);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 翻译操作日志类型
     */
    public String getOperLogTypeLabel(String type) {
        if (type == null) return null;
        return getMessage("operlog.type." + type, type);
    }
}
