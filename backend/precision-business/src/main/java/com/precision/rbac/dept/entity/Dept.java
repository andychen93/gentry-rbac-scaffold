package com.precision.rbac.dept.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.precision.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体，映射 sys_dept 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dept")
public class Dept extends TenantEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long parentId;
    private String ancestors;
    private String name;
    private Long leaderId;
    private String leaderName;
    private String phone;
    private String email;
    private Integer sort;
    private Integer status;
    // tenantId, createBy, createTime, updateBy, updateTime, deleted 由 TenantEntity 基类提供
}
