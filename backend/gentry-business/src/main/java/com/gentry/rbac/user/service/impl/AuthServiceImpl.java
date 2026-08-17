package com.gentry.rbac.user.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.constant.TenantConstants;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.UserContext;
import com.gentry.core.util.IpUtil;
import com.gentry.rbac.dept.entity.Dept;
import com.gentry.rbac.dept.mapper.DeptMapper;
import com.gentry.rbac.role.entity.Role;
import com.gentry.rbac.role.mapper.RoleMapper;
import com.gentry.rbac.role.mapper.RoleMenuMapper;
import com.gentry.rbac.menu.entity.Menu;
import com.gentry.rbac.menu.mapper.MenuMapper;
import com.gentry.core.i18n.MenuI18nKeyResolver;
import com.gentry.rbac.menu.vo.MenuTreeVO;
import com.gentry.rbac.tenant.entity.Tenant;
import com.gentry.rbac.tenant.mapper.TenantMapper;
import com.gentry.rbac.user.dto.LoginDTO;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.mapper.UserMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import com.gentry.rbac.user.service.AuthService;
import com.gentry.rbac.user.service.CaptchaService;
import com.gentry.rbac.user.service.LoginFailCounterService;
import com.gentry.core.config.CaptchaProperties;
import com.gentry.core.config.LoginSecurityProperties;
import com.gentry.core.config.PasswordProperties;
import com.gentry.rbac.config.SysConfigResolver;
import com.gentry.rbac.user.vo.LoginVO;
import com.gentry.rbac.user.vo.UserInfoVO;
import com.gentry.rbac.log.service.LogService;
import com.gentry.core.security.TokenBlacklistService;
import cn.dev33.satoken.session.SaSession;
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
    private final CaptchaService captchaService;
    private final LoginFailCounterService loginFailCounterService;
    private final CaptchaProperties captchaProperties;
    private final LoginSecurityProperties loginSecurityProperties;
    private final PasswordProperties passwordProperties;
    /** 运行时参数（sys_config 优先，yml 兜底） */
    private final SysConfigResolver sysConfigResolver;

    public AuthServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper, RoleMenuMapper roleMenuMapper,
                           TenantMapper tenantMapper, MenuMapper menuMapper,
                           DeptMapper deptMapper,
                           PasswordEncoder passwordEncoder,
                           LogService logService,
                           CaptchaService captchaService,
                           LoginFailCounterService loginFailCounterService,
                           CaptchaProperties captchaProperties,
                           LoginSecurityProperties loginSecurityProperties,
                           PasswordProperties passwordProperties,
                           SysConfigResolver sysConfigResolver) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.tenantMapper = tenantMapper;
        this.menuMapper = menuMapper;
        this.deptMapper = deptMapper;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
        this.captchaService = captchaService;
        this.loginFailCounterService = loginFailCounterService;
        this.captchaProperties = captchaProperties;
        this.loginSecurityProperties = loginSecurityProperties;
        this.passwordProperties = passwordProperties;
        this.sysConfigResolver = sysConfigResolver;
    }

    @Override
    public LoginVO login(LoginDTO dto, String loginIp, String userAgent) {
        // 0. 验证码校验（可配置开关；关闭时不校验）
        if (sysConfigResolver.getBoolean(SysConfigResolver.KEY_CAPTCHA_ENABLED, captchaProperties.isEnabled())) {
            captchaService.validate(dto.getUuid(), dto.getCaptcha());
        }

        // 1. 解析租户
        Long tenantId;
        try {
            tenantId = resolveTenantId(dto.getTenantCode());
        } catch (BizException e) {
            // 登录日志用 getFallbackMessage()（ErrorCode 里的中文常量）而不是 e.getMessage()：
            // 后者现在返回 i18n key，会把 error.tenant.not.found 写进日志表给运维看。
            // 与「@Log 推迟、操作/登录日志保持中文」的决定一致（概要设计 §4.8.1）。
            logService.saveLoginLog(dto.getUsername(), 0L, "password", loginIp, userAgent, 0, e.getFallbackMessage());
            throw e;
        }

        // 2. 账号锁定前置检查（连续失败达上限则拒绝，等 TTL 到期自动解锁）
        if (loginFailCounterService.isLocked(tenantId, dto.getUsername())) {
            int lockMinutes = loginFailCounterService.lockMinutes();
            // 登录日志保持中文（运维视角，与「日志不参与 i18n」一致）
            String msg = "账号已被锁定，请 " + lockMinutes + " 分钟后再试";
            logService.saveLoginLog(dto.getUsername(), tenantId, "password", loginIp, userAgent, 0, msg);
            // 给用户的消息带上具体分钟数：原来抛的是无参的「请稍后再试」，用户看不到还要等多久
            throw BizException.of(ErrorCode.ACCOUNT_LOCKED, lockMinutes);
        }

        // 3. 校验用户
        User user;
        try {
            user = findAndValidateUser(tenantId, dto.getUsername(), dto.getPassword());
        } catch (BizException e) {
            // 仅对「用户名或密码错误」计数（防爆破），用户禁用不计
            if (e.getCode() == ErrorCode.LOGIN_FAILED.getCode()) {
                loginFailCounterService.recordFail(tenantId, dto.getUsername());
            }
            logService.saveLoginLog(dto.getUsername(), tenantId, "password", loginIp, userAgent, 0, e.getFallbackMessage());
            throw e;
        }

        // 4. 构建登录结果 + 清除失败计数 + 密码过期标志 + 记录成功日志
        LoginVO result = buildLoginResult(user, tenantId, loginIp, userAgent);
        loginFailCounterService.clear(tenantId, dto.getUsername());
        result.setPasswordExpired(isPasswordExpired(user.getPwdUpdateTime()));
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

    /**
     * 判断密码是否过期。
     * <ul>
     *   <li>expireDays &lt;= 0：不校验</li>
     *   <li>pwdUpdateTime == null：视为未过期（容错种子数据，避免首次登录全员被强制改密）</li>
     * </ul>
     */
    private boolean isPasswordExpired(LocalDateTime pwdUpdateTime) {
        int expireDays = sysConfigResolver.getInt(
                SysConfigResolver.KEY_PASSWORD_EXPIRE_DAYS, passwordProperties.getExpireDays());
        if (expireDays <= 0) return false;
        if (pwdUpdateTime == null) return false;
        return pwdUpdateTime.plusDays(expireDays).isBefore(LocalDateTime.now());
    }

    // ========== Extract Method: 构建登录结果 ==========

    private LoginVO buildLoginResult(User user, Long tenantId, String loginIp, String userAgent) {
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 存储完整用户信息到 Session（供在线用户查询使用）
        // 注意：SaSession 底层是 ConcurrentHashMap，value 不能为 null，否则抛 NPE。
        //       因此可空字段（deptId/loginIp/location/nickname/...）走 putSession 跳过 null。
        SaSession session = StpUtil.getSession();
        session.set("tenantId", tenantId);
        session.set("userId", user.getId());
        putSession(session, "username", user.getUsername());
        putSession(session, "nickname", user.getNickname());
        putSession(session, "deptId", user.getDeptId());
        putSession(session, "loginIp", loginIp);
        session.set("loginTime", LocalDateTime.now());
        putSession(session, "location", IpUtil.getLocation(loginIp));

        // 解析 User-Agent 存入 Session（供在线用户查询使用）
        putSession(session, "browser", parseSimpleBrowser(userAgent));
        putSession(session, "os", parseSimpleOs(userAgent));

        // 查询部门名称存入 Session
        if (user.getDeptId() != null) {
            Dept dept = deptMapper.selectOneById(user.getDeptId());
            if (dept != null) {
                putSession(session, "deptName", dept.getName());
            }
        }

        // 先设置 UserContext，后续查询需要 tenantId
        UserContext.setUserId(user.getId());
        UserContext.setTenantId(tenantId);
        UserContext.setDeptId(user.getDeptId());

        // B-3: 登录时预加载 roles/permissions 到 Session
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        boolean platformAdmin = false;
        if (!roleIds.isEmpty()) {
            List<String> roleCodes = new ArrayList<>();
            for (Long roleId : roleIds) {
                Role r = roleMapper.selectOneById(roleId);
                if (r != null && r.getRoleCode() != null) roleCodes.add(r.getRoleCode());
            }
            // 平台超管识别：角色含 SUPER_ADMIN → 跨租户可见所有数据
            platformAdmin = roleCodes.contains(TenantConstants.PLATFORM_ROLE_CODE);
            List<String> permissions = roleMenuMapper.selectPermissionsByRoleIds(roleIds);
            session.set("roleList", roleCodes);
            session.set("permissionList", permissions);
        } else {
            session.set("roleList", new ArrayList<>());
            session.set("permissionList", new ArrayList<>());
        }
        session.set("platformAdmin", platformAdmin);
        UserContext.setPlatformAdmin(platformAdmin);
        // 语言偏好：null（用户从未选过）时不写 Session —— SaSession 底层是
        // ConcurrentHashMap 不接受 null value，且「不存在」正是「跟随浏览器」的语义
        putSession(session, "language", user.getLanguage());
        UserContext.setLanguage(user.getLanguage());

        userMapper.updateLoginInfo(user.getId(), loginIp, LocalDateTime.now());

        UserInfoVO userInfo = buildUserInfo(user);
        return new LoginVO(token, userInfo);
    }

    /** SaSession 底层是 ConcurrentHashMap，value 不能为 null；null 时跳过，避免登录 NPE。 */
    private static void putSession(SaSession session, String key, Object value) {
        if (value != null) {
            session.set(key, value);
        }
    }

    private UserInfoVO buildUserInfo(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setLanguage(user.getLanguage());   // 三态：null = 从未选过，前端保持当前 locale
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
        // 派生 i18n key 供前端翻译；permission 不下发（导航树不需要），但要用它算 key
        vo.setI18nKey(MenuI18nKeyResolver.resolve(menu.getPermission(), menu.getPath()));
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
