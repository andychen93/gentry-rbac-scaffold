package com.gentry.rbac.config.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数配置（全局表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_config")
public class Config extends BaseEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    /** 参数名称 */
    private String configName;
    /** 参数键（唯一，业务读取用） */
    private String configKey;
    /** 参数值 */
    private String configValue;
    /** 类型：Y=系统内置，N=业务自定义 */
    private String configType;
    private String remark;
}
