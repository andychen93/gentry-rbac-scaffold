package com.gentry.rbac.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户角色关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Table("sys_user_role")
public class UserRole extends BaseEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private Long userId;
    private Long roleId;

    public UserRole(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
    // createTime 由 BaseEntity 基类提供
}
