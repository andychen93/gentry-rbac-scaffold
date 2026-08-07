package com.precision.core.tenant;

import com.precision.core.constant.TenantConstants;
import com.precision.core.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多租户管理器测试（P1-04 租户隔离）。
 *
 * <p>验证：业务查询自动追加当前用户 tenant_id（不同租户互相隔离），
 * 未登录时回退默认租户，全局表不参与租户隔离。</p>
 */
@DisplayName("多租户管理器")
class PrecisionTenantManagerTest {

    private final PrecisionTenantManager manager = new PrecisionTenantManager();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("使用当前用户的 tenantId 作为隔离条件")
    void getTenantIds_usesCurrentUserTenant() {
        UserContext.setTenantId(96806963194000113L);

        Object[] ids = manager.getTenantIds();

        assertThat(ids).containsExactly(96806963194000113L);
    }

    @Test
    @DisplayName("不同租户得到各自的隔离 ID（互不串数据）")
    void getTenantIds_isolatesDifferentTenants() {
        UserContext.setTenantId(1001L);
        Object[] tenantA = manager.getTenantIds("biz_vehicle");

        UserContext.setTenantId(2002L);
        Object[] tenantB = manager.getTenantIds("biz_vehicle");

        assertThat(tenantA).containsExactly(1001L);
        assertThat(tenantB).containsExactly(2002L);
        assertThat(tenantA[0]).isNotEqualTo(tenantB[0]);
    }

    @Test
    @DisplayName("未登录（无 tenantId）回退默认租户")
    void getTenantIds_fallsBackToDefaultTenant() {
        UserContext.clear();

        Object[] ids = manager.getTenantIds();

        assertThat(ids).containsExactly(TenantConstants.DEFAULT_TENANT_ID);
    }

    @Test
    @DisplayName("getTenantIds(tableName) 与无参版本一致")
    void getTenantIds_byTable_sameAsNoArg() {
        UserContext.setTenantId(1001L);
        assertThat(manager.getTenantIds("biz_device")).containsExactly(1001L);
    }

    @Test
    @DisplayName("全局表在忽略列表中，不参与租户隔离")
    void ignoreTables_containsGlobalTables() {
        assertThat(manager.getIgnoreTables())
                .contains("sys_tenant", "sys_menu", "sys_role_menu", "sys_dict_type", "sys_dict_data");
    }

    @Test
    @DisplayName("业务表不在忽略列表中（强制租户隔离）")
    void ignoreTables_excludesBusinessTables() {
        assertThat(manager.getIgnoreTables())
                .doesNotContain("biz_vehicle", "biz_device", "biz_device_location", "biz_device_alarm");
    }
}
