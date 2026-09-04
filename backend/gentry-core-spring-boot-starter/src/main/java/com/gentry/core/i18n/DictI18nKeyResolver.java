package com.gentry.core.i18n;

import org.springframework.util.StringUtils;

/**
 * 由字典的编码列派生 i18n key。<b>纯计算，不查库、不落库。</b>
 *
 * <p>译文归<b>前端</b>语言包（{@code locales/{lang}/dict.json}）。实测种子数据 21 条
 * 字典项 100% 可派生（全部有 {@code dict_type} + {@code dict_value}）。</p>
 *
 * <p>本仓库已拿掉多租户机制（见 {@code doc/design/modules/core/去多租户化-概要设计.md}），
 * {@code sys_dict_data} / {@code sys_dict_type} 全局唯一，不再有「不同组织建出相同
 * {@code (dict_type, dict_value)} 导致派生 key 撞车」的顾虑。</p>
 *
 * <p>另有一条已知行为：管理员在字典管理页改了内置字典项的 label 后，
 * 因译文优先，界面不会变。前端的字典管理页对「语言包中存在该 key」的项禁用 label 输入并给
 * 提示，把约束摆在动作发生处。见前端详细设计 §6.3.1。</p>
 */
public final class DictI18nKeyResolver {

    private static final String PREFIX = "dict.";
    private static final String TYPE_PREFIX = "dict.type.";

    private DictI18nKeyResolver() {
    }

    /**
     * 字典项：{@code dict.{dictType}.{dictValue}}。
     *
     * <pre>
     * (sys_user_gender, 1) -> dict.sys_user_gender.1
     * (sys_user_post, CEO) -> dict.sys_user_post.CEO
     * </pre>
     *
     * @return i18n key；入参任一为空时返回 null，此时前端回退显示 dictLabel
     */
    public static String resolveData(String dictType, String dictValue) {
        if (!StringUtils.hasText(dictType) || !StringUtils.hasText(dictValue)) {
            return null;
        }
        return PREFIX + dictType.trim() + "." + dictValue.trim();
    }

    /**
     * 字典类型：{@code dict.type.{dictType}}。
     *
     * @return i18n key；入参为空时返回 null，此时前端回退显示 dictName
     */
    public static String resolveType(String dictType) {
        return StringUtils.hasText(dictType) ? TYPE_PREFIX + dictType.trim() : null;
    }
}
