package com.gentry.core.i18n;

import org.springframework.util.StringUtils;

/**
 * 由菜单已有的编码列派生 i18n key。<b>纯计算，不查库、不落库。</b>
 *
 * <p>这是本方案的核心取舍：不给 {@code sys_menu} 加 {@code i18n_key} 列，也不建翻译副表，
 * 而是复用早就存在的 {@code permission} / {@code path} 作为稳定标识符。实测种子数据
 * 71 条菜单 100% 可派生且无重复（3 目录靠 path、12 页面与 56 按钮靠 permission）。</p>
 *
 * <p>译文归<b>前端</b>语言包（{@code locales/{lang}/nav.json}），后端只负责把 key 随 VO
 * 下发。这样切换语言无需重新请求菜单接口。</p>
 *
 * <p><b>隐式依赖警告：</b>{@code permission} 一旦成为 i18n key 的来源，改动它会
 * <b>静默</b>让译文退化为库里的中文 {@code name}（不报错、不抛异常）。permission 本来就与
 * {@code @SaCheckPermission} 硬绑不应改动，但这条依赖不写下来没人知道，
 * 由 E2E 的 I18N-001 单向对账兜住。</p>
 */
public final class MenuI18nKeyResolver {

    private static final String PREFIX = "menu";

    private MenuI18nKeyResolver() {
    }

    /**
     * permission 优先，为空时退 path；两者皆空返回 {@code null}。
     *
     * <pre>
     * system:user:add  -> menu.system.user.add
     * system:user      -> menu.system.user
     * /system          -> menu.system
     * /monitor-center  -> menu.monitor-center
     * </pre>
     *
     * @return i18n key；无法派生时返回 null，此时前端回退显示库里的 name
     */
    public static String resolve(String permission, String path) {
        if (StringUtils.hasText(permission)) {
            return PREFIX + "." + normalize(permission.trim(), ':');
        }
        if (StringUtils.hasText(path)) {
            // path 以 / 开头，normalize 后天然带上分隔点：/system -> .system
            return PREFIX + normalize(path.trim(), '/');
        }
        return null;
    }

    /**
     * 把分隔符换成点，并折叠连续的点、去掉尾点。
     *
     * <p>折叠是必要的：{@code /system/user} 这类多段 path 正常，但 {@code //system} 或
     * 结尾带斜杠的 {@code /system/} 会产出 {@code menu..system} / {@code menu.system.}
     * 这种畸形 key —— 它们不会报错，只会永远查不到译文。</p>
     */
    private static String normalize(String raw, char separator) {
        String dotted = raw.replace(separator, '.');
        StringBuilder sb = new StringBuilder(dotted.length());
        char prev = 0;
        for (int i = 0; i < dotted.length(); i++) {
            char c = dotted.charAt(i);
            if (c == '.' && prev == '.') {
                continue;                 // 折叠连续点
            }
            sb.append(c);
            prev = c;
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '.') {
            sb.setLength(sb.length() - 1);   // 去尾点
        }
        return sb.toString();
    }
}
