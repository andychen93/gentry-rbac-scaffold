package com.gentry.core.tenant;

import com.mybatisflex.core.tenant.TenantFactory;
import com.gentry.core.constant.TenantConstants;
import com.gentry.core.security.UserContext;

import java.util.Set;

/**
 * 租户管理器
 * <p>
 * 三层租户跳过机制：
 * <ol>
 *   <li>ignoreTables() — 表级别忽略（如 sys_tenant, sys_menu 等全局表）</li>
 *   <li>TenantManager.ignoreTenantCondition() — 方法级别跳过</li>
 *   <li>getTenantIds() — 动态获取当前用户租户 ID</li>
 * </ol>
 */
public class GentryTenantManager implements TenantFactory {

    /**
     * 不需要租户隔离的全局表（平台级数据）
     */
    private static final Set<String> IGNORE_TABLES = Set.of(
        "sys_tenant", "sys_menu", "sys_role_menu", "sys_dict_type", "sys_dict_data", "sys_config"
    );

    @Override
    public Object[] getTenantIds() {
        // 平台超管：跨租户可见所有数据（TenantFactory 返回 null 即跳过租户条件追加）
        if (UserContext.isPlatformAdmin()) {
            return null;
        }
        Long tenantId = UserContext.getTenantId();
        return new Object[]{tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT_ID};
    }

    @Override
    public Object[] getTenantIds(String tableName) {
        return getTenantIds();
    }

    /**
     * 获取忽略租户条件的表名集合
     */
    public Set<String> getIgnoreTables() {
        return IGNORE_TABLES;
    }
}
