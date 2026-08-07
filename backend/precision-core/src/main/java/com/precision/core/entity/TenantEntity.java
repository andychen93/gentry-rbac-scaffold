package com.precision.core.entity;

import com.mybatisflex.annotation.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户实体基类，继承 BaseEntity 并包含 tenantId
 * 所有租户级业务表继承此类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantEntity extends BaseEntity {

    @Column(tenantId = true)
    private Long tenantId;
}
