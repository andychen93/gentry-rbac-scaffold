package com.gentry.rbac.dict.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体，映射 sys_dict_data 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_dict_data")
public class DictData extends TenantEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private String cssClass;
    private String listClass;
    private Integer isDefault;
    private Integer sort;
    private Integer status;
    private String remark;
    // tenantId, createBy, createTime, updateBy, updateTime, deleted 由 TenantEntity 基类提供
}
