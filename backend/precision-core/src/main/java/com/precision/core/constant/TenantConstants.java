package com.precision.core.constant;

/**
 * 租户常量
 *
 * TODO [多租户演进] 开放多租户时需要：
 *  1. 引入"平台超管"角色（跨租户，管理租户本身的增删改查）
 *  2. 新增"平台租户"概念（tenant_id=0 或 is_platform 标识），超管账号归属于此
 *  3. 超管登录后进入租户管理界面，租户管理员登录后进入业务界面
 *  4. 当前阶段所有权限都是租户级 RBAC，不存在跨租户操作
 */
public class TenantConstants {

    /** 默认租户 ID（写死在代码中，对应 sys_tenant.id = 1） */
    public static final Long DEFAULT_TENANT_ID = 1L;

    private TenantConstants() {}
}
