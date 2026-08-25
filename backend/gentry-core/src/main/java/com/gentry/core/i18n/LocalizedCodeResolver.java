package com.gentry.core.i18n;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 把「人填进 Excel 的一格文字」归一成稳定的编码。
 *
 * <p><b>为什么需要它。</b>导出是给人看的报表，性别列写「男」、职务列写「经理」；
 * 导入模板是给人填的，表头写「性别(0未知1男2女)」提示填码。两边语义不同，
 * 于是<b>导出的文件导不回来</b>——这是本类要解决的既有缺陷：
 * {@code UserExportVO.gender} 是写着「男」的 String，而 {@code UserImportDTO.gender}
 * 曾是 Integer，导回去那一列静默变 null，且 {@code UserImportDTO} 的类注释还写着
 * 「中英文任意语言导出的文件都能导回来」。</p>
 *
 * <p>解决办法不是把导出改成写码（那会让中文用户打开 Excel 看到一堆英文标识），
 * 而是让导入<b>同时认码和任一语言的 label</b>：</p>
 *
 * <pre>
 * "1" | "男" | "Male"                 -> "1"
 * "Manager" | "经理" | "manager"      -> "Manager"
 * </pre>
 *
 * <p>于是三种来源都能导：模板里手填的码、中文导出的文件、英文导出的文件。</p>
 *
 * <p><b>匹配顺序是「先码后 label」</b>，不能反。若某个 label 恰好等于另一个码
 * （字典码用英文单词时很容易发生，例如 label「Manager」与码 {@code Manager}），
 * 先匹配码能保证结果稳定，而不是取决于遍历顺序。</p>
 */
@Component
public class LocalizedCodeResolver {

    private final I18nUtil i18nUtil;
    /** 语言白名单，与 {@link I18nProperties} 同源。用 LinkedHashSet 保序，让匹配结果可复现 */
    private final Set<Locale> supportedLocales;

    public LocalizedCodeResolver(I18nUtil i18nUtil, I18nProperties props) {
        this.i18nUtil = i18nUtil;
        Set<Locale> set = new LinkedHashSet<>();
        for (String raw : props.getSupportedLocales()) {
            parseLocale(raw).ifPresent(set::add);
        }
        if (set.isEmpty()) {
            set.add(Locale.SIMPLIFIED_CHINESE);
        }
        this.supportedLocales = Set.copyOf(set);
    }

    /**
     * @param codeToMessageKey 码 → 该码的译文 i18n key，顺序即匹配顺序
     * @param raw              Excel 单元格原文，可为 null / 空白
     * @return 归一后的码；输入空白或认不出时返回 {@code null}，由调用方决定是报错还是留空
     */
    public String resolve(Map<String, String> codeToMessageKey, String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();

        // ① 先按码精确匹配（大小写无关：运营手填 manager 也该认）
        for (String code : codeToMessageKey.keySet()) {
            if (code.equalsIgnoreCase(value)) {
                return code;
            }
        }

        // ② 再按任一受支持语言的 label 匹配 —— 这是「导出的文件能导回来」的关键
        for (Locale locale : supportedLocales) {
            for (Map.Entry<String, String> e : codeToMessageKey.entrySet()) {
                String label = i18nUtil.getMessage(e.getValue(), locale, null);
                if (label != null && label.trim().equalsIgnoreCase(value)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /** {@code zh_CN} / {@code zh-CN} 都接受；非法值忽略 */
    private static Optional<Locale> parseLocale(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        try {
            Locale locale = Locale.forLanguageTag(raw.trim().replace('_', '-'));
            return locale.getLanguage().isEmpty() ? Optional.empty() : Optional.of(locale);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
