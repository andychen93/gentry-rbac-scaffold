package com.precision.rbac.user.service.impl;

import com.precision.core.common.ErrorCode;
import com.precision.core.constant.TenantConstants;
import com.precision.core.exception.BizException;
import com.precision.core.security.UserContext;
import com.precision.core.util.IpUtil;
import com.precision.rbac.dept.entity.Dept;
import com.precision.rbac.dept.mapper.DeptMapper;
import com.precision.rbac.role.entity.Role;
import com.precision.rbac.role.mapper.RoleMapper;
import com.precision.rbac.role.mapper.RoleMenuMapper;
import com.precision.rbac.menu.entity.Menu;
import com.precision.rbac.menu.mapper.MenuMapper;
import com.precision.rbac.menu.vo.MenuTreeVO;
import com.precision.rbac.tenant.entity.Tenant;
import com.precision.rbac.tenant.mapper.TenantMapper;
import com.precision.rbac.user.dto.LoginDTO;
import com.precision.rbac.user.entity.User;
import com.precision.rbac.user.mapper.UserMapper;
import com.precision.rbac.user.mapper.UserRoleMapper;
import com.precision.rbac.user.service.AuthService;
import com.precision.rbac.user.vo.LoginVO;
import com.precision.rbac.user.vo.UserInfoVO;
import com.precision.rbac.log.service.LogService;
import com.precision.core.security.TokenBlacklistService;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final TenantMapper tenantMapper;
    private final MenuMapper menuMapper;
    private final DeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    public AuthServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper, RoleMenuMapper roleMenuMapper,
                           TenantMapper tenantMapper, MenuMapper menuMapper,
                           DeptMapper deptMapper,
                           PasswordEncoder passwordEncoder,
                           LogService logService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.tenantMapper = tenantMapper;
        this.menuMapper = menuMapper;
        this.deptMapper = deptMapper;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    @Override
    public LoginVO login(LoginDTO dto, String loginIp, String userAgent) {
        // 1. 解析租户
        Long tenantId;
        try {
            tenantId = resolveTenantId(dto.getTenantCode());
        } catch (BizException e) {
            logService.saveLoginLog(dto.getUsername(), 0L, "password", loginIp, userAgent, 0, e.getMessage());
            throw e;
        }

        // 2. 校验用户
        User user;
        try {
            user = findAndValidateUser(tenantId, dto.getUsername(), dto.getPassword());
        } catch (BizException e) {
            logService.saveLoginLog(dto.getUsername(), tenantId, "password", loginIp, userAgent, 0, e.getMessage());
            throw e;
        }

        // 3. 构建登录结果 + 记录成功日志
        LoginVO result = buildLoginResult(user, tenantId, loginIp, userAgent);
        logService.saveLoginLog(user.getUsername(), tenantId, "password", loginIp, userAgent, 1, "登录成功");
        return result;
    }

    @Override
    public void logout() {
        // JWT 模式下：登出前先把当前 Token 加入黑名单，防止 Token 在过期前被重放
        try {
            String tokenValue = StpUtil.getTokenValue();
            if (tokenValue != null) {
                TokenBlacklistService.addToBlacklist(tokenValue);
            }
        } catch (Exception e) {
            log.warn("Failed to add token to blacklist on logout: {}", e.getMessage());
        }
        StpUtil.logout();
    }

    @Override
    public UserInfoVO getUserInfo() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return buildUserInfo(user);
    }

    // ========== Extract Method: 租户解析 ==========

    // TODO [多租户演进] resolveTenantId() 是策略接口的雏形，
    //  未来租户解析策略增多（域名解析、Header 解析等）时，升级为 TenantResolver.resolve()

    /**
     * 解析租户ID。
     * - tenantCode 为空 → 默认租户（常量，不查库，不校验状态）
     * - tenantCode 不为空 → 查 sys_tenant，校验状态和过期
     */
    private Long resolveTenantId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return TenantConstants.DEFAULT_TENANT_ID;
        }
        Tenant tenant = tenantMapper.selectByCode(tenantCode);
        if (tenant == null) {
            throw new BizException(ErrorCode.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() != 1) {
            throw new BizException(ErrorCode.TENANT_DISABLED);
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.TENANT_EXPIRED);
        }
        return tenant.getId();
    }

    // ========== Extract Method: 用户校验 ==========

    private User findAndValidateUser(Long tenantId, String username, String password) {
        User user = userMapper.selectByUsername(tenantId, username);
        if (user == null) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() != 1) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        return user;
    }

    // ========== Extract Method: 构建登录结果 ==========

    private LoginVO buildLoginResult(User user, Long tenantId, String loginIp, String userAgent) {
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 存储完整用户信息到 Session（供在线用户查询使用）
        StpUtil.getSession().set("tenantId", tenantId);
        StpUtil.getSession().set("userId", user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("nickname", user.getNickname());
        StpUtil.getSession().set("deptId", user.getDeptId());
        StpUtil.getSession().set("loginIp", loginIp);
        StpUtil.getSession().set("loginTime", LocalDateTime.now());
        StpUtil.getSession().set("location", IpUtil.getLocation(loginIp));

        // 解析 User-Agent 存入 Session（供在线用户查询使用）
        StpUtil.getSession().set("browser", parseSimpleBrowser(userAgent));
        StpUtil.getSession().set("os", parseSimpleOs(userAgent));

        // 查询部门名称存入 Session
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectOneById(user.getDeptId());
            if (dept != null) {
                StpUtil.getSession().set("deptName", dept.getName());
            }
        }

        // 先设置 UserContext，后续查询需要 tenantId
        UserContext.setUserId(user.getId());
        UserContext.setTenantId(tenantId);
        UserContext.setDeptId(user.getDeptId());

        // B-3: 登录时预加载 roles/permissions 到 Session
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        if (!roleIds.isEmpty()) {
            List<String> roleCodes = new ArrayList<>();
            for (Long roleId : roleIds) {
                Role r = roleMapper.selectOneById(roleId);
                if (r != null && r.getRoleCode() != null) roleCodes.add(r.getRoleCode());
            }
            List<String> permissions = roleMenuMapper.selectPermissionsByRoleIds(roleIds);
            StpUtil.getSession().set("roleList", roleCodes);
            StpUtil.getSession().set("permissionList", permissions);
        } else {
            StpUtil.getSession().set("roleList", new ArrayList<>());
            StpUtil.getSession().set("permissionList", new ArrayList<>());
        }

        userMapper.updateLoginInfo(user.getId(), loginIp, LocalDateTime.now());

        UserInfoVO userInfo = buildUserInfo(user);
        return new LoginVO(token, userInfo);
    }

    private UserInfoVO buildUserInfo(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setDeptId(user.getDeptId());

        // 填充部门名称
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectOneById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }

        // B-4: 填充 roleCode/roleName/dataScope
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        List<UserInfoVO.RoleInfo> roles = new ArrayList<>();
        if (!roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                Role role = roleMapper.selectOneById(roleId);
                if (role != null) {
                    roles.add(new UserInfoVO.RoleInfo(
                        role.getId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getDataScope()
                    ));
                }
            }
        }
        vo.setRoles(roles);

        // B-5: 从数据库查询权限列表
        if (!roleIds.isEmpty()) {
            List<String> permissions = roleMenuMapper.selectPermissionsByRoleIds(roleIds);
            vo.setPermissions(permissions != null ? permissions : new ArrayList<>());
        } else {
            vo.setPermissions(new ArrayList<>());
        }

        // B-6: 构建用户菜单树（仅 type=1 目录 + type=2 菜单，用于前端动态路由和侧边栏渲染）
        if (!roleIds.isEmpty()) {
            List<MenuTreeVO> menuTree = buildUserMenuTree(roleIds);
            vo.setMenus(menuTree);
        } else {
            vo.setMenus(Collections.emptyList());
        }

        return vo;
    }

    /**
     * 根据角色ID列表构建用户可访问的菜单树。
     * 1. 查询角色关联的菜单ID
     * 2. 查询菜单实体（type IN 1,2，status=1，visible=1）
     * 3. 自动补全父目录（向上遍历 parentId 链路）
     * 4. 构建树结构
     */
    private List<MenuTreeVO> buildUserMenuTree(List<Long> roleIds) {
        // 查询角色关联的菜单ID
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询关联菜单（type IN 1,2，status=1，visible=1）
        List<Menu> menus = menuMapper.selectNavByIds(menuIds);
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集已有ID，用于去重
        Set<Long> ownedIds = menus.stream().map(Menu::getId).collect(Collectors.toSet());

        // 向上补全父目录
        Set<Long> missingParentIds = new HashSet<>();
        for (Menu m : menus) {
            if (m.getParentId() != 0L && !ownedIds.contains(m.getParentId())) {
                missingParentIds.add(m.getParentId());
            }
        }
        while (!missingParentIds.isEmpty()) {
            List<Menu> parents = menuMapper.selectByIds(new ArrayList<>(missingParentIds));
            Set<Long> nextMissing = new HashSet<>();
            for (Menu p : parents) {
                if (!ownedIds.contains(p.getId())) {
                    menus.add(p);
                    ownedIds.add(p.getId());
                    if (p.getParentId() != 0L && !ownedIds.contains(p.getParentId())) {
                        nextMissing.add(p.getParentId());
                    }
                }
            }
            missingParentIds = nextMissing;
        }

        // 转换为 MenuTreeVO 并构建树
        List<MenuTreeVO> voList = menus.stream()
                .map(this::toNavTreeVO)
                .collect(Collectors.toList());
        return buildMenuTree(voList);
    }

    private MenuTreeVO toNavTreeVO(Menu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setIcon(menu.getIcon());
        vo.setType(menu.getType());
        vo.setSort(menu.getSort());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setVisible(menu.getVisible());
        return vo;
    }

    private List<MenuTreeVO> buildMenuTree(List<MenuTreeVO> voList) {
        Map<Long, List<MenuTreeVO>> childrenMap = voList.stream()
                .collect(Collectors.groupingBy(MenuTreeVO::getParentId));
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO vo : voList) {
            vo.setChildren(childrenMap.getOrDefault(vo.getId(), Collections.emptyList()));
            if (vo.getParentId() == 0L) {
                roots.add(vo);
            }
        }
        roots.sort(Comparator.comparingInt(a -> a.getSort() != null ? a.getSort() : 0));
        return roots;
    }

    // ========== User-Agent 简单解析（供 Session 存储） ==========

    private String parseSimpleBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edge")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        return "Unknown";
    }

    private String parseSimpleOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "Mac OS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iOS") || ua.contains("iPhone")) return "iOS";
        return "Unknown";
    }
}
