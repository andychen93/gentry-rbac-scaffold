package com.gentry.rbac.security;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.gentry.rbac.role.mapper.RoleMenuMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限/角色接口实现
 *
 * 通过 SaSession 懒加载并缓存用户的权限列表和角色编码列表。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final Logger log = LoggerFactory.getLogger(StpInterfaceImpl.class);

    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public StpInterfaceImpl(UserRoleMapper userRoleMapper, RoleMenuMapper roleMenuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return Collections.emptyList();
        }

        Object cached = session.get("permissionList");
        if (cached != null) {
            return (List<String>) cached;
        }

        long userId = Long.parseLong(loginId.toString());
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            session.set("permissionList", Collections.emptyList());
            return Collections.emptyList();
        }

        List<String> permissions = roleMenuMapper.selectPermissionsByRoleIds(roleIds);
        session.set("permissionList", permissions);
        return permissions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId, false);
        if (session == null) {
            return Collections.emptyList();
        }

        Object cached = session.get("roleList");
        if (cached != null) {
            return (List<String>) cached;
        }

        long userId = Long.parseLong(loginId.toString());
        List<String> roleCodes = userRoleMapper.selectRoleCodesByUserId(userId);
        session.set("roleList", roleCodes);
        return roleCodes;
    }

    /**
     * 清除指定用户的权限/角色缓存
     */
    public void clearUserCache(Long userId) {
        SaSession session = StpUtil.getSessionByLoginId(userId, false);
        if (session != null) {
            session.delete("permissionList");
            session.delete("roleList");
            log.debug("Cleared permission cache for user [{}]", userId);
        }
    }
}
