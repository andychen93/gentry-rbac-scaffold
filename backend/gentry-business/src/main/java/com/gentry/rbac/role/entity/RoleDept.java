package com.gentry.rbac.role.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 角色部门关联实体，映射 sys_role_dept 表
 * 全局关联表（无 tenant_id），不继承基类
 */
@Data
@NoArgsConstructor
@Table("sys_role_dept")
public class RoleDept {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long roleId;
    private Long deptId;
    private LocalDateTime createTime;

    public RoleDept(Long id, Long roleId, Long deptId) {
        this.id = id;
        this.roleId = roleId;
        this.deptId = deptId;
    }
}
