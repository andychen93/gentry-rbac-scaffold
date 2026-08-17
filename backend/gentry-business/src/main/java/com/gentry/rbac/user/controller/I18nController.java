package com.gentry.rbac.user.controller;

import com.gentry.core.common.R;
import com.gentry.core.i18n.I18nProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国际化元数据。供前端渲染语言选择器。
 */
@RestController
@RequestMapping("/api/v1/i18n")
public class I18nController {

    /**
     * 语言的<b>自称</b>。语言选择器里显示自称是通行做法——否则英文用户看到「中文」
     * 反而不认识。所以这份 label 不随当前 locale 翻译，是常量。
     */
    private static final Map<String, String> NATIVE_NAMES = Map.of(
            "zh_CN", "简体中文",
            "en_US", "English"
    );

    private final I18nProperties props;

    public I18nController(I18nProperties props) {
        this.props = props;
    }

    /**
     * 支持的语言列表。不加权限注解：未登录的登录页也要渲染语言选择器。
     */
    @GetMapping("/locales")
    public R<List<Map<String, String>>> locales() {
        List<Map<String, String>> list = new ArrayList<>();
        for (String code : props.getSupportedLocales()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", code);
            item.put("label", NATIVE_NAMES.getOrDefault(code, code));
            list.add(item);
        }
        return R.ok(list);
    }
}
