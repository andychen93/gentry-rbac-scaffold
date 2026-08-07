package com.precision.rbac.role.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.precision.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体，映射 sys_role 表
 * 租户级数据（含 tenant_id）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class Role extends TenantEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String roleCode;
    private String roleName;
    private Integer dataScope;
    private Integer sort;
    private Integer status;
    private String remark;
    // tenantId, createBy, createTime, updateBy, updateTime, deleted 由 TenantEntity 基类提供
}
