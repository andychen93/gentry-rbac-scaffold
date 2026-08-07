package com.precision.core.security;

/**
 * 用户上下文（ThreadLocal）。
 * <p>由 SaTokenConfig 的拦截器在请求进入时从 Sa-Token Session 恢复，请求结束时清理。</p>
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }

    public static void setTenantId(Long tenantId) { TENANT_ID.set(tenantId); }
    public static Long getTenantId() { return TENANT_ID.get(); }

    public static void setDeptId(Long deptId) { DEPT_ID.set(deptId); }
    public static Long getDeptId() { return DEPT_ID.get(); }

    public static void clear() {
        USER_ID.remove();
        TENANT_ID.remove();
        DEPT_ID.remove();
    }
}
