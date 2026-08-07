package com.precision.rbac.tenant.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.precision.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户实体（全局表，无 tenant_id）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_tenant")
public class Tenant extends BaseEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String logo;
    private String domain;
    private Long packageId;
    private LocalDateTime expireTime;
    private Integer accountLimit;
    private Integer deviceLimit;
    private Integer status;
    private String remark;
    /** JSONB config stored as String, parsed in Service layer */
    private String config;
    // createBy, createTime, updateBy, updateTime, deleted 由 BaseEntity 基类提供
}
