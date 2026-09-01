package com.gentry.core.entity;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体基类，提供公共字段。所有业务表继承此类。
 *
 * <p>本仓库已拿掉多租户机制（见
 * {@code doc/design/modules/core/去多租户化-概要设计.md}），此前区分「全局表继承
 * {@code BaseEntity}、租户表继承 {@code TenantEntity}」的两层基类结构已合并为一层——
 * 不再存在需要额外 {@code tenantId} 字段的表。</p>
 */
@Data
public abstract class BaseEntity {

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer deleted;
}
