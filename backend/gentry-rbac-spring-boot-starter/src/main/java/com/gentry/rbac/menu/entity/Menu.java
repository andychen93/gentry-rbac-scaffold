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
    /**
     * 是否平台级权限点：1 = 只能由平台超管（SUPER_ADMIN）持有与分配，0 = 租户级。
     *
     * <p>判据放在菜单表而不是角色上：「这个权限点是平台级的」是权限点<b>自身</b>的属性。
     * 于是两个角色的定义可推导 —— SUPER_ADMIN = 全部菜单，租户 ADMIN = 全部
     * {@code is_platform = 0} 的菜单。见
     * {@code doc/design/modules/rbac/modules/平台级权限隔离/详细设计.md}。</p>
     */
    private Integer isPlatform;
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
