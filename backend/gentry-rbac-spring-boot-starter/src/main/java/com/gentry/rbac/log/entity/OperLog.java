package com.gentry.rbac.log.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体，映射 sys_oper_log 表。
 * 不继承 BaseEntity/TenantEntity（日志表无 createTime/updateTime/deleted 等审计字段）。
 * 租户隔离通过全局配置 setTenantColumn("tenant_id") 自动生效，
 * 所有 BaseMapper 的增删改查都会自动追加 tenant_id 条件。
 */
@Data
@Table("sys_oper_log")
public class OperLog {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long tenantId;
    private String module;
    private String type;
    private String title;
    private String operator;
    private Long operatorId;
    private String operatorIp;
    private String location;
    private String method;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private Integer status;
    private String errorMsg;
    private Integer costTime;
    private LocalDateTime operateTime;
}
