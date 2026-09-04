package com.gentry.rbac.menu.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单实体，映射 sys_menu 表
 * 全局表（无 tenant_id），字段结构与 BaseEntity 不完全匹配（无 createBy/updateBy），不继承基类
 */
@Data
@Table("sys_menu")
public class Menu {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer type;
    private Integer sort;
    private String permission;
    private String path;
    private String component;
    private Integer visible;
    private Integer status;
    private Integer isExternal;
    private Integer isCache;
    // active / query 是历史遗留字段，sys_menu 表无对应列（见建表迁移），忽略持久化，
    // 否则 MyBatis-Flex 的 insert/update 会带上这两列导致 "Unknown column 'active'"。
    @Column(ignore = true)
    private String active;
    @Column(ignore = true)
    private String query;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @Column(isLogicDelete = true)
    private Integer deleted;
}
