package com.gentry.rbac.tenant.dto;

import jakarta.validation.constraints.Min;

/**
 * 租户扩展配置。序列化成 JSON 存进 {@code sys_tenant.config}（TEXT 列）。
 *
 * <p><b>这个 DTO 就是 config 的字段白名单</b> —— {@code TenantServiceImpl.updateConfig}
 * 只写这里声明的键，不做增量合并。原因见下。</p>
 *
 * <p>原来还有 {@code maxDevices} / {@code dataRetentionDays} /
 * {@code videoEnabled} / {@code alarmEnabled} / {@code reportEnabled} / {@code mapProvider}
 * 六个字段，是本仓库派生自车辆定位平台时留下的业务概念，与通用 RBAC 脚手架无关，
 * 已由 {@code V13__cleanup_business_leftovers.sql} 连同 {@code device_limit} 列一起清掉。
 * 派生项目要加自己的租户级配置，往本类加字段即可，无需改表。</p>
 */
public class TenantConfigDTO {

    /** 该租户允许的最大用户数。null 表示不修改 */
    @Min(value = 1, message = "{valid.tenant.maxUsers.min}")
    private Integer maxUsers;

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
}
