package com.gentry.rbac.role.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.UserContext;
import com.gentry.core.util.IdGenerator;
import com.gentry.rbac.role.dto.*;
import com.gentry.rbac.role.entity.Role;
import com.gentry.rbac.role.entity.RoleDept;
import com.gentry.rbac.role.entity.RoleMenu;
import com.gentry.rbac.role.enums.DataScope;
import com.gentry.rbac.role.mapper.RoleDeptMapper;
import com.gentry.rbac.role.mapper.RoleMapper;
import com.gentry.rbac.role.mapper.RoleMenuMapper;
import com.gentry.rbac.role.service.RoleService;
import com.gentry.rbac.role.vo.RoleDetailVO;
import com.gentry.rbac.role.vo.RoleListVO;
import com.gentry.rbac.role.vo.RoleOptionVO;
import com.gentry.rbac.role.vo.RoleVO;
import com.gentry.rbac.user.entity.UserRole;
import com.gentry.rbac.user.mapper.UserMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);
    private static final Set<String> BUILTIN_CODES = Set.of("ADMIN", "USER");

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RoleDeptMapper roleDeptMapper;
    private final com.gentry.rbac.dept.mapper.DeptMapper deptMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;

    public RoleServiceImpl(RoleMapper roleMapper,
                           RoleMenuMapper roleMenuMapper,
                           RoleDeptMapper roleDeptMapper,
                           com.gentry.rbac.dept.mapper.DeptMapper deptMapper,
                           UserRoleMapper userRoleMapper,
                           UserMapper userMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleDeptMapper = roleDeptMapper;
        this.deptMapper = deptMapper;
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Long createAdminRole(Long tenantId, String roleCode, String roleName) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDataScope(DataScope.ALL.getCode());
        role.setSort(0);
        role.setStatus(1);
        roleMapper.insert(role);
        log.info("Created admin role: id={}, tenantId={}, code={}", role.getId(), tenantId, roleCode);
        return role.getId();
    }

    @Override
    public List<RoleOptionVO> listOptions() {
        Long tenantId = UserContext.getTenantId();
        return roleMapper.selectOptions(tenantId);
    }

    @Override
    public PageResult<RoleListVO> list(RoleQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        int offset = (query.getPageNum() - 1) * query.getPageSize();

        List<Map<String, Object>> rows = roleMapper.selectPageList(
                tenantId, query.getRoleName(), query.getRoleCode(), query.getStatus(),
                offset, query.getPageSize());
        long total = roleMapper.selectCount(
                tenantId, query.getRoleName(), query.getRoleCode(), query.getStatus());

        List<RoleListVO> list = rows.stream().map(this::mapToListVO).collect(Collectors.toList());
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public RoleDetailVO getDetail(Long id) {
        Role role = getExistingRole(id);
        List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleId(id);
        List<Long> deptIds = roleDeptMapper.selectDeptIdsByRoleId(id);

        RoleDetailVO vo = new RoleDetailVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDataScope(role.getDataScope());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setMenuIds(menuIds);
        vo.setDeptIds(deptIds);
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
        return vo;
    }

    @Override
    @Transactional
    public RoleVO create(RoleCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();

        // 禁止使用内置编码
        if (BUILTIN_CODES.contains(dto.getRoleCode().toUpperCase())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.role.builtin.code.reserved");
        }

        // 校验编码唯一
        if (roleMapper.countByCode(tenantId, dto.getRoleCode()) > 0) {
            throw new BizException(ErrorCode.ROLE_CODE_EXISTS);
        }

        Role role = new Role();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDataScope(dto.getDataScope() != null ? dto.getDataScope() : DataScope.ALL.getCode());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        role.setRemark(dto.getRemark());
        roleMapper.insert(role);

        log.info("Created role: id={}, code={}", role.getId(), role.getRoleCode());
        return toVO(role);
    }

    @Override
    @Transactional
    public void update(Long id, RoleUpdateDTO dto) {
        getExistingRole(id);

        Role role = new Role();
        role.setId(id);
        // roleCode 不可修改，不设置
        // dataScope/status 通过专用接口修改，不在编辑接口中设置
        role.setRoleName(dto.getRoleName());
        role.setSort(dto.getSort());
        role.setRemark(dto.getRemark());
        roleMapper.update(role);

        log.info("Updated role: id={}", id);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        Role role = getExistingRole(id);

        // 校验非内置角色
        if (BUILTIN_CODES.contains(role.getRoleCode().toUpperCase())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.role.builtin.undeletable");
        }

        // 校验无关联用户
        int userCount = roleMenuMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BizException(ErrorCode.ROLE_IN_USE);
        }

        // 事务内：逻辑删除角色 + 物理删除关联
        roleMapper.logicDeleteById(id);
        roleMenuMapper.deleteByRoleId(id);
        roleDeptMapper.deleteByRoleId(id);

        log.info("Deleted role: id={}, code={}", id, role.getRoleCode());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, RoleMenuAssignDTO dto) {
        getExistingRole(roleId);

        // D-05: 校验 menuId 存在性
        List<Long> menuIds = dto.getMenuIds();
        if (menuIds != null && !menuIds.isEmpty()) {
            List<Long> allMenuIds = roleMenuMapper.selectAllMenuIds();
            List<Long> invalidIds = menuIds.stream()
                    .filter(id -> !allMenuIds.contains(id))
                    .collect(Collectors.toList());
            if (!invalidIds.isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.menu.id.not.found", invalidIds);
            }

            /*
             * **平台级权限点只能由平台超管分配。**
             *
             * 原来这里只校验「menuId 存不存在」，不校验「调用者有没有权分配它」。
             * 而租户管理员握有 system:role:assignMenu —— 它能在「角色管理 → 权限」里
             * 把 system:tenant:* 勾给自己的角色，从而管理所有租户。
             * 所以「新建租户时不给平台级权限」只是必要不充分，这条守卫才是承重的。
             *
             * 为什么不用 @SaCheckPermission：注解只能表达「调用者有没有 X 权限」，
             * 表达不了「调用者**提交的数据**里有没有越权内容」。这属于业务规则。
             *
             * 判据 UserContext.isPlatformAdmin() 是既有机制：登录时由 AuthServiceImpl
             * 按 roleCodes.contains("SUPER_ADMIN") 算好写进 Session。
             */
            if (!UserContext.isPlatformAdmin()) {
                List<Long> platformIds = roleMenuMapper.selectPlatformMenuIdsIn(menuIds);
                if (!platformIds.isEmpty()) {
                    /*
                     * 越权的菜单 ID 只进服务端日志，不进给用户的报错文案：
                     * 前端已按 isPlatform 隐藏这些节点，能走到这里的要么是过期页面、
                     * 要么是手工构造请求 —— 后者不该被告知「哪几个是平台级」。
                     * 而日志里要留下审计痕迹（谁、哪个角色、试了哪些权限点）。
                     */
                    log.warn("Rejected platform-menu assignment: userId={}, tenantId={}, roleId={}, menuIds={}",
                            UserContext.getUserId(), UserContext.getTenantId(), roleId, platformIds);
                    throw new BizException(ErrorCode.PLATFORM_MENU_FORBIDDEN);
                }
            }
        }

        // 先删后插
        roleMenuMapper.deleteByRoleId(roleId);

        if (menuIds != null && !menuIds.isEmpty()) {
            List<RoleMenu> list = menuIds.stream()
                    .map(menuId -> new RoleMenu(IdGenerator.nextId(), roleId, menuId))
                    .collect(Collectors.toList());
            roleMenuMapper.batchInsert(list);
        }

        log.info("Assigned menus to role: roleId={}, menuCount={}", roleId,
                menuIds != null ? menuIds.size() : 0);
    }

    @Override
    @Transactional
    public void updateDataScope(Long roleId, RoleDataScopeDTO dto) {
        getExistingRole(roleId);

        int dataScope = dto.getDataScope();

        // CUSTOM 时校验 deptIds 非空
        if (dataScope == DataScope.CUSTOM.getCode()) {
            if (dto.getDeptIds() == null || dto.getDeptIds().isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.role.custom.scope.dept.required");
            }
            // D-06: 校验 deptId 存在性
            for (Long deptId : dto.getDeptIds()) {
                if (deptMapper.selectOneById(deptId) == null) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.id.not.found", deptId);
                }
            }
        }

        // 更新 data_scope
        roleMapper.updateDataScope(roleId, dataScope);

        // 先删后插 sys_role_dept
        roleDeptMapper.deleteByRoleId(roleId);
        if (dataScope == DataScope.CUSTOM.getCode() && dto.getDeptIds() != null && !dto.getDeptIds().isEmpty()) {
            List<RoleDept> list = dto.getDeptIds().stream()
                    .map(deptId -> new RoleDept(IdGenerator.nextId(), roleId, deptId))
                    .collect(Collectors.toList());
            roleDeptMapper.batchInsert(list);
        }

        log.info("Updated data scope for role: roleId={}, dataScope={}", roleId, dataScope);
    }

    @Override
    public void updateStatus(Long id, RoleStatusDTO dto) {
        getExistingRole(id);
        roleMapper.updateStatus(id, dto.getStatus());
        log.info("Updated role status: id={}, status={}", id, dto.getStatus());
    }

    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        getExistingRole(roleId);
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUsers(Long roleId, RoleUserAssignDTO dto) {
        Role role = getExistingRole(roleId);
        List<Long> userIds = dto.getUserIds() == null ? List.of() : dto.getUserIds();

        // 去重：前端穿梭框理论上不会给重复值，但 userIds 直接进唯一约束，
        // 重复会整批插入失败，在这里挡掉比让用户看数据库报错友好
        List<Long> distinctIds = userIds.stream().distinct().collect(Collectors.toList());

        // 校验用户存在且属于本租户 —— 与 UserServiceImpl.validateRoleIds 同理：
        // 不校验就会把不存在的 userId 写进 sys_user_role 变成脏关联，
        // 之后每次打开绑定弹窗都带着一个界面上看不见、又提交必失败的 id
        if (!distinctIds.isEmpty()) {
            Long tenantId = UserContext.getTenantId();
            int existing = userMapper.countExistingByIds(tenantId, distinctIds);
            if (existing != distinctIds.size()) {
                throw new BizException(ErrorCode.USER_NOT_FOUND,
                        "存在无效用户，或用户不属于当前租户");
            }
        }

        // 全量覆盖：先解除该角色的所有关联，再按本次列表重建
        userRoleMapper.deleteByRoleId(roleId);
        if (!distinctIds.isEmpty()) {
            Long tenantId = UserContext.getTenantId();
            List<UserRole> rows = distinctIds.stream().map(uid -> {
                UserRole ur = new UserRole(uid, roleId);
                ur.setId(IdGenerator.nextId());
                ur.setTenantId(tenantId);
                return ur;
            }).collect(Collectors.toList());
            userRoleMapper.batchInsert(rows);
        }

        log.info("Assigned users to role: roleId={}, code={}, userCount={}",
                roleId, role.getRoleCode(), distinctIds.size());
    }

    // ========== 私有方法 ==========

    private Role getExistingRole(Long id) {
        Role role = roleMapper.selectOneById(id);
        if (role == null) {
            throw new BizException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private RoleListVO mapToListVO(Map<String, Object> row) {
        RoleListVO vo = new RoleListVO();
        vo.setId(((Number) row.get("id")).longValue());
        vo.setRoleCode((String) row.get("roleCode"));
        vo.setRoleName((String) row.get("roleName"));
        Integer ds = row.get("dataScope") != null ? ((Number) row.get("dataScope")).intValue() : 1;
        vo.setDataScope(ds);
        try {
            vo.setDataScopeName(DataScope.fromCode(ds).getDescription());
        } catch (IllegalArgumentException e) {
            vo.setDataScopeName("未知");
        }
        vo.setUserCount(row.get("userCount") != null ? ((Number) row.get("userCount")).intValue() : 0);
        vo.setSort(row.get("sort") != null ? ((Number) row.get("sort")).intValue() : 0);
        vo.setStatus(row.get("status") != null ? ((Number) row.get("status")).intValue() : 1);
        Object ct = row.get("createTime");
        if (ct instanceof LocalDateTime) {
            vo.setCreateTime((LocalDateTime) ct);
        } else if (ct instanceof java.sql.Timestamp) {
            vo.setCreateTime(((java.sql.Timestamp) ct).toLocalDateTime());
        }
        return vo;
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDataScope(role.getDataScope());
        vo.setSort(role.getSort());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
