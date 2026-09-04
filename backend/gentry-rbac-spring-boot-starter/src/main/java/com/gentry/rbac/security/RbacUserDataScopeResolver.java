package com.gentry.rbac.security;

import com.gentry.core.data.UserDataScopeResolver;
import com.gentry.rbac.role.entity.Role;
import com.gentry.rbac.role.mapper.RoleMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link UserDataScopeResolver} 的默认实现：
 * 查询用户绑定的所有角色，取 data_scope 最小值作为用户的最终权限范围（越小越宽松）。
 */
@Component
public class RbacUserDataScopeResolver implements UserDataScopeResolver {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public RbacUserDataScopeResolver(UserRoleMapper userRoleMapper, RoleMapper roleMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public Integer resolveDataScope(Long userId) {
        if (userId == null) return null;
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) return null;

        List<Role> roles = roleMapper.selectByIds(roleIds);
        if (roles == null || roles.isEmpty()) return null;

        Integer minScope = null;
        for (Role r : roles) {
            if (r.getDataScope() == null) continue;
            if (minScope == null || r.getDataScope() < minScope) {
                minScope = r.getDataScope();
            }
        }
        return minScope;
    }
}
