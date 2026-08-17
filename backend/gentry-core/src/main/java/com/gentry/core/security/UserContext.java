package com.gentry.core.security;

/**
 * 用户上下文（ThreadLocal）。
 * <p>由 SaTokenConfig 的拦截器在请求进入时从 Sa-Token Session 恢复，请求结束时清理。</p>
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM_ADMIN = new ThreadLocal<>();
    private static final ThreadLocal<String> LANGUAGE = new ThreadLocal<>();

    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }

    public static void setTenantId(Long tenantId) { TENANT_ID.set(tenantId); }
    public static Long getTenantId() { return TENANT_ID.get(); }

    public static void setDeptId(Long deptId) { DEPT_ID.set(deptId); }
    public static Long getDeptId() { return DEPT_ID.get(); }

    /** 是否平台超管（跨租户，由 roleCode=SUPER_ADMIN 判定，登录时写入 Session） */
    public static void setPlatformAdmin(boolean platformAdmin) { PLATFORM_ADMIN.set(platformAdmin); }
    public static boolean isPlatformAdmin() {
        Boolean v = PLATFORM_ADMIN.get();
        return v != null && v;
    }

    /**
     * 用户语言偏好，形如 {@code zh_CN} / {@code en_US}。
     *
     * <p><b>null 有明确语义</b>：未登录，或已登录但用户从未选择过语言
     * （{@code sys_user.language IS NULL}）。此时语言解析会退到
     * {@code Accept-Language} 请求头，见 {@code GentryLocaleResolver}。</p>
     */
    public static void setLanguage(String language) { LANGUAGE.set(language); }
    public static String getLanguage() { return LANGUAGE.get(); }

    /**
     * 清理全部 ThreadLocal。
     *
     * <p><b>新增字段必须同步加进来。</b>线程池复用下漏清理的后果是「上一个 en_US
     * 用户的语言泄漏给下一个用户」，且只在并发下偶发，极难排查。</p>
     */
    public static void clear() {
        USER_ID.remove();
        TENANT_ID.remove();
        DEPT_ID.remove();
        PLATFORM_ADMIN.remove();
        LANGUAGE.remove();
    }
}
