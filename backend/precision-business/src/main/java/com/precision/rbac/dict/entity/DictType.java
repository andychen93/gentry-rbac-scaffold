package com.precision.rbac.dict.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.precision.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型实体，映射 sys_dict_type 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dict_type")
public class DictType extends TenantEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;
    // tenantId, createBy, createTime, updateBy, updateTime, deleted 由 TenantEntity 基类提供
}
