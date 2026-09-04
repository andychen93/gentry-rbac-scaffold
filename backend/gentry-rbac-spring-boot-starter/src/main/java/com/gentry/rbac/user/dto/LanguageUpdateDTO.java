package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 语言偏好更新入参。
 */
public class LanguageUpdateDTO {

    /**
     * 语言码，形如 {@code zh_CN} / {@code en_US}。
     *
     * <p><b>这里只校验格式，不写语言白名单。</b>注解常量读不到配置，若在此写
     * {@code ^(zh_CN|en_US)$}，加一门语言就得同时改 {@code gentry.i18n.supported-locales}
     * 和这个正则——漏改一处的表现是「配置里加了语言但接口拒绝保存」。
     * 白名单校验在 Service 里查 {@code I18nProperties}，保持单一真源。</p>
     */
    @NotBlank(message = "{valid.user.language.notBlank}")
    @Pattern(regexp = "^[a-zA-Z]{2}(_[a-zA-Z]{2,4})?$", message = "{valid.user.language.pattern}")
    private String language;

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
