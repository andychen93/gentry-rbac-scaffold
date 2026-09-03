package com.gentry.core.i18n;

import org.springframework.util.StringUtils;

/**
 * 由角色的编码列派生 i18n key。<b>纯计算，不查库、不落库。</b>
 *
 * <p>与 {@link MenuI18nKeyResolver} / {@link DictI18nKeyResolver} 同一套取舍：不给
 * {@code sys_role} 加列，复用早就存在且稳定的 {@code role_code} 作为标识符。
 * 译文归<b>前端</b>语言包（{@code locales/{lang}/role.json}）。</p>
 *
 * <p>只对<b>内置角色</b>（{@code ADMIN}/{@code USER}，见
 * {@code RoleServiceImpl.BUILTIN_CODES}）真正生效：运营自建角色的 {@code roleName}
 * 是任意文本，派生出的 key 语言包里必然查不到，前端会回退显示库里的原始
 * {@code roleName}——这是可接受的降级，不是 bug（与字典管理页「语言包接管内置项」
 * 的处理方式同构）。</p>
 */
public final class RoleI18nKeyResolver {

    private static final String PREFIX = "role.";

    private RoleI18nKeyResolver() {
    }

    /**
     * {@code role.{roleCode 小写}}。
     *
     * <pre>
     * ADMIN -> role.admin
     * USER  -> role.user
     * </pre>
     *
     * @return i18n key；roleCode 为空时返回 null，此时前端回退显示 roleName
     */
    public static String resolve(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return null;
        }
        return PREFIX + roleCode.trim().toLowerCase();
    }
}
