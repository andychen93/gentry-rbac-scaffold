package com.gentry.rbac.tenant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.TokenBlacklistService;
import com.gentry.core.util.IdGenerator;
import com.gentry.rbac.dept.service.DeptService;
import com.gentry.rbac.role.mapper.RoleMenuMapper;
import com.gentry.rbac.role.service.RoleService;
import com.gentry.rbac.tenant.dto.*;
import com.gentry.rbac.tenant.entity.Tenant;
import com.gentry.rbac.tenant.mapper.TenantMapper;
import com.gentry.rbac.tenant.service.TenantService;
import com.gentry.rbac.tenant.vo.*;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.entity.UserRole;
import com.gentry.rbac.user.mapper.UserMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantServiceImpl implements TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ADMIN_USERNAME = "admin";
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@$!%*?&";

    private final TenantMapper tenantMapper;
    private final DeptService deptService;
    private final RoleService roleService;
    private final RoleMenuMapper roleMenuMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public TenantServiceImpl(TenantMapper tenantMapper,
                             DeptService deptService,
                             RoleService roleService,
                             RoleMenuMapper roleMenuMapper,
                             UserMapper userMapper,
                             UserRoleMapper userRoleMapper,
                             PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.deptService = deptService;
        this.roleService = roleService;
        this.roleMenuMapper = roleMenuMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<TenantListVO> list(TenantQueryDTO query) {
        long total = tenantMapper.selectCount(query);
        List<TenantListVO> list = Collections.emptyList();
        if (total > 0) {
            list = tenantMapper.selectList(query);
        }
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public TenantDetailVO getDetail(Long id) {
        Tenant tenant = getExistingTenant(id);
        TenantDetailVO vo = toDetailVO(tenant);
        vo.setStatistics(tenantMapper.selectStatistics(id));
        return vo;
    }

    @Override
    @Transactional
    public TenantCreateResultVO create(TenantCreateDTO dto) {
        // 1. 校验租户编码唯一
        if (tenantMapper.selectByCode(dto.getCode()) != null) {
            throw new BizException(ErrorCode.TENANT_CODE_EXISTS);
        }

        // 2. 创建租户（ID 由 @Id 注解自动生成，createBy/updateBy 由 AutoFillHandler 自动填充）
        Tenant tenant = new Tenant();
        tenant.setCode(dto.getCode());
        tenant.setName(dto.getName());
        tenant.setContact(dto.getContact());
        tenant.setPhone(dto.getPhone());
        tenant.setEmail(dto.getEmail());
        tenant.setExpireTime(dto.getExpireTime());
        tenant.setRemark(dto.getRemark());
        tenant.setAccountLimit(100);
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        Long tenantId = tenant.getId();

        // 3. 创建默认部门
        Long deptId = deptService.createDefaultDept(tenantId, dto.getName());

        // 4. 创建管理员角色
        Long roleId = roleService.createAdminRole(tenantId, "ADMIN", "管理员");

        // 5. 分配**租户级**菜单权限给管理员角色（= 租户下的最高权限）
        assignTenantScopedMenusToRole(roleId);

        // 6. 创建 admin 用户
        String rawPassword = generateRandomPassword();
        Long adminUserId = createAdminUser(tenantId, deptId, rawPassword);

        // 7. 分配管理员角色给 admin 用户
        assignRoleToUser(tenantId, adminUserId, roleId);

        log.info("Tenant created: id={}, code={}", tenantId, dto.getCode());
        return new TenantCreateResultVO(tenantId, dto.getCode(), ADMIN_USERNAME, rawPassword);
    }

    @Override
    @Transactional
    public void update(Long id, TenantUpdateDTO dto) {
        getExistingTenant(id);

        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(dto.getName());
        tenant.setContact(dto.getContact());
        tenant.setPhone(dto.getPhone());
        tenant.setEmail(dto.getEmail());
        tenant.setExpireTime(dto.getExpireTime());
        tenant.setRemark(dto.getRemark());
        tenantMapper.update(tenant);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getExistingTenant(id);

        // 校验租户下仅允许存在 admin 用户
        int userCount = tenantMapper.countUsersByTenantId(id);
        if (userCount > 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.tenant.has.users");
        }

        tenantMapper.logicDeleteById(id);
    }

    @Override
    @Transactional
    public void updateConfig(Long id, TenantConfigDTO dto) {
        Tenant tenant = getExistingTenant(id);

        /*
         * **白名单写入，不做增量合并。**
         *
         * 原实现在既有 JSON 上逐键 put，好处是不认识的键不会丢；坏处是删掉一个配置项之后，
         * 历史 JSON 里的旧键会永久留着，并继续通过 TenantDetailVO.config 下发给前端 ——
         * 车辆监控那批字段（maxDevices / features / mapProvider）就是这么留下来的。
         *
         * 改成只写 TenantConfigDTO 声明的键之后，「代码里没有的字段」会在下一次保存时
         * 自动消失，config 的字段清单永远等于 DTO。
         *
         * 仍要读一遍旧值：null 表示「本次不修改这一项」，与「显式清空」不同，
         * 未提交的项要原样带过去。
         */
        Map<String, Object> existing = parseConfig(tenant.getConfig());
        Map<String, Object> configMap = new LinkedHashMap<>();
        Integer maxUsers = dto.getMaxUsers() != null
                ? dto.getMaxUsers()
                : asInteger(existing == null ? null : existing.get("maxUsers"));
        if (maxUsers != null) {
            configMap.put("maxUsers", maxUsers);
        }

        try {
            String configJson = OBJECT_MAPPER.writeValueAsString(configMap);
            tenantMapper.updateConfig(id, configJson);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "error.tenant.config.serialize.failed");
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, TenantStatusDTO dto) {
        getExistingTenant(id);
        tenantMapper.updateStatus(id, dto.getStatus());

        // 禁用时踢出该租户所有在线用户
        if (dto.getStatus() == 0) {
            List<Long> userIds = tenantMapper.selectUserIdsByTenantId(id);
            for (Long userId : userIds) {
                // JWT 模式：将用户所有 Token 加入黑名单
                try {
                    TokenBlacklistService.blacklistAllTokensOfUser(userId);
                } catch (Exception e) {
                    log.warn("Failed to blacklist tokens of user {}: {}", userId, e.getMessage());
                }
                try {
                    StpUtil.kickout(userId);
                } catch (Exception e) {
                    log.warn("Failed to kickout user {}: {}", userId, e.getMessage());
                }
            }
            log.info("Disabled tenant {} and kicked out {} users", id, userIds.size());
        }
    }

    @Override
    public List<TenantOptionVO> listOptions() {
        return tenantMapper.selectOptions();
    }

    // ========== 私有方法 ==========

    private Tenant getExistingTenant(Long id) {
        Tenant tenant = tenantMapper.selectOneById(id);
        if (tenant == null) {
            throw new BizException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    /**
     * 给新租户的 ADMIN 角色分配「租户下的最高权限」。
     *
     * <p><b>这里原来调的是 {@code selectAllMenuIds()}</b>，注释也写着「分配所有菜单权限」——
     * 于是每个新建租户的管理员都拿到了 {@code system:tenant:*}，能列出/新增/修改/删除
     * <b>所有</b>租户，包括默认租户。而种子数据里的 ADMIN（role_id=1）是对的，
     * E2E 也只测了它，两条路径给出的权限不一致，测试正好覆盖了对的那条。</p>
     *
     * <p>现在按 {@code sys_menu.is_platform} 过滤。注意这只堵住「新建时给多了」，
     * 租户管理员自己在角色管理里勾回来那条路由 {@code RoleServiceImpl.assignMenus}
     * 的守卫堵 —— 只修这里是安全剧场。</p>
     */
    private void assignTenantScopedMenusToRole(Long roleId) {
        List<Long> menuIds = roleMenuMapper.selectTenantScopedMenuIds();
        if (menuIds.isEmpty()) {
            log.warn("No tenant-scoped menus found to assign to role {}", roleId);
            return;
        }
        List<RoleMenuMapper.RoleMenuEntry> entries = menuIds.stream()
                .map(menuId -> new RoleMenuMapper.RoleMenuEntry(IdGenerator.nextId(), roleId, menuId))
                .collect(Collectors.toList());
        roleMenuMapper.batchInsertRoleMenu(entries);
    }

    private Long createAdminUser(Long tenantId, Long deptId, String rawPassword) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(ADMIN_USERNAME);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname("管理员");
        user.setDeptId(deptId);
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    private void assignRoleToUser(Long tenantId, Long userId, Long roleId) {
        UserRole ur = new UserRole(userId, roleId);
        ur.setId(IdGenerator.nextId());
        ur.setTenantId(tenantId);
        userRoleMapper.batchInsert(List.of(ur));
    }

    private TenantDetailVO toDetailVO(Tenant tenant) {
        TenantDetailVO vo = new TenantDetailVO();
        vo.setId(tenant.getId());
        vo.setCode(tenant.getCode());
        vo.setName(tenant.getName());
        vo.setContact(tenant.getContact());
        vo.setPhone(tenant.getPhone());
        vo.setEmail(tenant.getEmail());
        vo.setAddress(tenant.getAddress());
        vo.setLogo(tenant.getLogo());
        vo.setDomain(tenant.getDomain());
        vo.setExpireTime(tenant.getExpireTime());
        vo.setAccountLimit(tenant.getAccountLimit());
        vo.setStatus(tenant.getStatus());
        vo.setRemark(tenant.getRemark());
        vo.setConfig(parseConfig(tenant.getConfig()));
        vo.setCreateTime(tenant.getCreateTime());
        vo.setUpdateTime(tenant.getUpdateTime());
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(configJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tenant config JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JSON 反序列化出来的数字可能是 Integer/Long/Double，统一归一成 Integer。
     * 认不出的类型返回 null，而不是抛异常 —— 配置是软状态，一个脏值不该让保存整体失败。
     */
    private Integer asInteger(Object raw) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof String str && !str.isBlank()) {
            try {
                return Integer.valueOf(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        // Ensure at least one uppercase, one lowercase, one digit
        sb.append((char) ('A' + random.nextInt(26)));
        sb.append((char) ('a' + random.nextInt(26)));
        sb.append((char) ('0' + random.nextInt(10)));
        for (int i = 3; i < 10; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        // Shuffle
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

}
