package com.gentry.rbac.role.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色菜单关联实体，映射 sys_role_menu 表
 * 全局关联表（无 tenant_id），不继承基类
 */
@Data
@NoArgsConstructor
@Table("sys_role_menu")
public class RoleMenu {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long roleId;
    private Long menuId;
    private LocalDateTime createTime;

    public RoleMenu(Long id, Long roleId, Long menuId) {
        this.id = id;
        this.roleId = roleId;
        this.menuId = menuId;
    }
}
