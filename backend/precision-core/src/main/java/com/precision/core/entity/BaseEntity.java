package com.precision.core.entity;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体基类，提供公共字段
 * 所有表（全局表 + 租户表）都包含这些字段
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
