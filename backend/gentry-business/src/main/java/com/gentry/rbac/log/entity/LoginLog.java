package com.gentry.rbac.log.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体，映射 sys_login_log 表。
 * 不继承 BaseEntity/TenantEntity（日志表无 createTime/updateTime/deleted 等审计字段）。
 * 租户隔离通过全局配置 setTenantColumn("tenant_id") 自动生效，
 * 所有 BaseMapper 的增删改查都会自动追加 tenant_id 条件。
 */
@Data
@Table("sys_login_log")
public class LoginLog {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long tenantId;
    private String username;
    private String loginType;
    private String loginIp;
    private String location;
    private String browser;
    private String os;
    private String deviceType;
    private String userAgent;
    private Integer status;
    private String message;
    private LocalDateTime loginTime;
}
