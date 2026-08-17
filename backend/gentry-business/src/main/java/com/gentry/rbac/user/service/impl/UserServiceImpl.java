package com.gentry.rbac.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.TokenBlacklistService;
import com.gentry.core.security.UserContext;
import com.gentry.core.util.IdGenerator;
import com.gentry.rbac.dept.mapper.DeptMapper;
import com.gentry.rbac.dept.service.DeptService;
import com.gentry.rbac.role.entity.Role;
import com.gentry.rbac.role.mapper.RoleMapper;
import com.gentry.rbac.user.dto.*;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.entity.UserRole;
import com.gentry.rbac.user.mapper.UserMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import com.gentry.rbac.user.service.UserService;
import com.gentry.rbac.user.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String ADMIN_USERNAME = "admin";
    /** 导入用户的默认初始密码（符合密码强度规则，用户首次登录后可自行修改） */
    private static final String DEFAULT_IMPORT_PASSWORD = "Abc@123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;
    private final DeptService deptService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper, DeptMapper deptMapper,
                           DeptService deptService,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
        this.deptService = deptService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDetailVO create(UserCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();

        // 校验用户名不能是 admin
        if (ADMIN_USERNAME.equals(dto.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.username.reserved");
        }

        // 校验用户名租户内唯一
        if (userMapper.countByUsername(tenantId, dto.getUsername()) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        // 校验手机号租户内唯一
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.countByPhone(tenantId, dto.getPhone(), null) > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.user.phone.in.use");
            }
        }

        // 构建 User 实体（ID 由 @Id 注解自动生成，tenantId/createBy 由 AutoFillHandler 自动填充）
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender() != null ? dto.getGender() : 0);
        user.setPostName(dto.getPostName());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        user.setRemark(dto.getRemark());

        userMapper.insert(user);

        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            validateRoleIds(dto.getRoleIds());
            batchInsertUserRoles(user.getId(), dto.getRoleIds());
        }

        return getDetail(user.getId());
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        Long tenantId = UserContext.getTenantId();
        User existing = getExistingUser(id);

        // 校验手机号唯一（排除自身）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.countByPhone(tenantId, dto.getPhone(), id) > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.user.phone.in.use");
            }
        }

        // 不能将当前登录用户禁用
        if (dto.getStatus() != null && dto.getStatus() == 0 && id.equals(UserContext.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.cannot.disable.self");
        }

        // 不能禁用 admin
        if (dto.getStatus() != null && dto.getStatus() == 0 && ADMIN_USERNAME.equals(existing.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.admin.cannot.disable");
        }

        User user = new User();
        user.setId(id);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender());
        user.setPostName(dto.getPostName());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus());
        user.setRemark(dto.getRemark());

        userMapper.update(user);

        // 更新角色（先删后插）
        if (dto.getRoleIds() != null) {
            // 必须校验：create 与 assignRoles 都校验了，这里漏掉会把不存在的 roleId
            // 直接写进 sys_user_role 变成脏关联（历史上就是这么进来的），
            // 之后该用户每次「分配角色」都会被 validateRoleIds 拦下且无法自愈
            if (!dto.getRoleIds().isEmpty()) {
                validateRoleIds(dto.getRoleIds());
            }
            userRoleMapper.deleteByUserId(id);
            if (!dto.getRoleIds().isEmpty()) {
                batchInsertUserRoles(id, dto.getRoleIds());
            }
        }
    }

    @Override
    @Transactional
    public void remove(Long id) {
        User user = getExistingUser(id);

        // 不能删除当前登录用户
        if (id.equals(UserContext.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.cannot.delete.self");
        }

        // 不能删除 admin
        if (ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.admin.cannot.delete");
        }

        userMapper.logicDeleteById(id);
        userRoleMapper.deleteByUserId(id);

        // B-12: 删除用户后踢出在线会话
        // JWT 模式：同时将该用户所有有效 Token 加入黑名单
        try {
            TokenBlacklistService.blacklistAllTokensOfUser(id);
        } catch (Exception e) {
            log.warn("Failed to blacklist tokens of user {} after deletion: {}", id, e.getMessage());
        }
        try {
            StpUtil.kickout(id);
        } catch (Exception e) {
            log.warn("Failed to kickout user {} after deletion: {}", id, e.getMessage());
        }
    }

    @Override
    public UserDetailVO getDetail(Long id) {
        User user = getExistingUser(id);
        return toDetailVO(user);
    }

    @Override
    public PageResult<UserListVO> list(UserQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        List<Long> deptIds = null;

        // 如果指定了部门，查询该部门及子部门
        if (query.getDeptId() != null) {
            deptIds = deptService.getChildDeptIds(query.getDeptId());
        }

        long total = userMapper.selectCount(query, tenantId, deptIds);
        List<User> users = Collections.emptyList();
        if (total > 0) {
            users = userMapper.selectList(query, tenantId, deptIds);
        }

        List<UserListVO> voList = users.stream().map(this::toListVO).collect(Collectors.toList());
        return new PageResult<>(voList, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional
    public void resetPassword(Long id, UserPasswordResetDTO dto) {
        getExistingUser(id);
        String encoded = passwordEncoder.encode(dto.getNewPassword());
        userMapper.updatePassword(id, encoded, LocalDateTime.now());

        // B-13: 重置密码后踢出在线会话，强制重新登录
        // JWT 模式：将该用户所有有效 Token 加入黑名单，防止过期前被重放
        try {
            TokenBlacklistService.blacklistAllTokensOfUser(id);
        } catch (Exception e) {
            log.warn("Failed to blacklist tokens of user {} after password reset: {}", id, e.getMessage());
        }
        try {
            StpUtil.kickout(id);
        } catch (Exception e) {
            log.warn("Failed to kickout user {} after password reset: {}", id, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updatePassword(UserPasswordUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        User user = getExistingUser(userId);

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.OLD_PASSWORD_ERROR);
        }

        String encoded = passwordEncoder.encode(dto.getNewPassword());
        userMapper.updatePassword(userId, encoded, LocalDateTime.now());

        // JWT 模式：改密码后将该用户所有有效 Token 加入黑名单 + 踢出在线会话，强制重新登录
        try {
            TokenBlacklistService.blacklistAllTokensOfUser(userId);
        } catch (Exception e) {
            log.warn("Failed to blacklist tokens of user {} after self password update: {}", userId, e.getMessage());
        }
        try {
            StpUtil.kickout(userId);
        } catch (Exception e) {
            log.warn("Failed to kickout user {} after self password update: {}", userId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateProfile(UserProfileUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        Long tenantId = UserContext.getTenantId();
        getExistingUser(userId);

        // 手机号唯一校验（排除自身）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.countByPhone(tenantId, dto.getPhone(), userId) > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.user.phone.in.use");
            }
        }

        // 部分更新：MyBatis-Flex update 忽略 null 字段，用户未填的字段不会被覆盖
        User user = new User();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender());
        user.setPostName(dto.getPostName());
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long id, UserRoleAssignDTO dto) {
        getExistingUser(id);
        userRoleMapper.deleteByUserId(id);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            validateRoleIds(dto.getRoleIds());
            batchInsertUserRoles(id, dto.getRoleIds());
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long id, UserStatusDTO dto) {
        User user = getExistingUser(id);

        // 不能禁用当前登录用户
        if (dto.getStatus() == 0 && id.equals(UserContext.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.cannot.disable.self");
        }

        // 不能禁用 admin
        if (dto.getStatus() == 0 && ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.admin.cannot.disable");
        }

        userMapper.updateStatus(id, dto.getStatus());
    }

    // ========== 私有方法 ==========

    private User getExistingUser(Long id) {
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void batchInsertUserRoles(Long userId, List<Long> roleIds) {
        Long tenantId = UserContext.getTenantId();
        List<UserRole> userRoles = roleIds.stream().map(roleId -> {
            UserRole ur = new UserRole(userId, roleId);
            ur.setId(IdGenerator.nextId());
            ur.setTenantId(tenantId);
            return ur;
        }).collect(Collectors.toList());
        userRoleMapper.batchInsert(userRoles);
    }

    @Override
    public List<UserOptionVO> listOptions() {
        return userMapper.selectOptions(UserContext.getTenantId());
    }

    private void validateRoleIds(List<Long> roleIds) {
        List<Role> roles = roleMapper.selectByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            List<Long> missing = roleIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BizException(ErrorCode.ROLE_NOT_FOUND, "error.role.not.found.detail", missing);
        }
    }

    private UserListVO toListVO(User user) {
        UserListVO vo = new UserListVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setPostName(user.getPostName());
        vo.setDeptId(user.getDeptId());
        // 填充部门名称
        if (user.getDeptId() != null) {
            var dept = deptMapper.selectOneById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        // 填充角色详情（含 roleName/roleCode）
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectByIds(roleIds);
            vo.setRoles(roles.stream()
                    .map(r -> new RoleVO(r.getId(), r.getRoleName(), r.getRoleCode()))
                    .collect(Collectors.toList()));
        } else {
            vo.setRoles(List.of());
        }
        return vo;
    }

    private UserDetailVO toDetailVO(User user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setPostName(user.getPostName());
        vo.setAvatar(user.getAvatar());
        vo.setDeptId(user.getDeptId());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginDate(user.getLoginDate());
        vo.setPwdUpdateTime(user.getPwdUpdateTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        vo.setRoleIds(roleIds);
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectByIds(roleIds);
            vo.setRoles(roles.stream()
                    .map(r -> new RoleVO(r.getId(), r.getRoleName(), r.getRoleCode()))
                    .collect(Collectors.toList()));
        } else {
            vo.setRoles(List.of());
        }
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // ========== 导入导出 ==========

    @Override
    public void exportUsers(UserQueryDTO query, HttpServletResponse response) throws IOException {
        Long tenantId = UserContext.getTenantId();
        List<Long> deptIds = query.getDeptId() != null ? deptService.getChildDeptIds(query.getDeptId()) : null;
        List<User> users = userMapper.selectList(query, tenantId, deptIds);
        List<UserExportVO> rows = users.stream().map(this::toExportVO).collect(Collectors.toList());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");
        EasyExcel.write(response.getOutputStream(), UserExportVO.class).sheet("用户列表").doWrite(rows);
    }

    @Override
    public UserImportResultVO importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.import.file.empty");
        }
        List<UserImportDTO> rows;
        try (InputStream is = file.getInputStream()) {
            rows = EasyExcel.read(is).head(UserImportDTO.class).sheet().doReadSync();
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.import.file.read.failed");
        }
        int success = 0, fail = 0;
        List<UserImportResultVO.ErrorItem> errors = new ArrayList<>();
        // 逐行导入：单行失败不影响其他行。create 内 self-invocation 的 @Transactional 不生效，
        // 但导入不分配角色（roleIds 为空），仅单条 insert user，原子操作，安全。
        for (int i = 0; i < rows.size(); i++) {
            UserImportDTO row = rows.get(i);
            String username = row.getUsername();
            try {
                if (!StringUtils.hasText(username)) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "error.user.import.username.blank");
                }
                UserCreateDTO dto = new UserCreateDTO();
                dto.setUsername(username.trim());
                dto.setNickname(StringUtils.hasText(row.getNickname()) ? row.getNickname() : username.trim());
                dto.setPhone(row.getPhone());
                dto.setEmail(row.getEmail());
                dto.setGender(row.getGender() != null ? row.getGender() : 0);
                dto.setPostName(row.getPostName());
                dto.setPassword(DEFAULT_IMPORT_PASSWORD);
                dto.setStatus(1);
                create(dto);
                success++;
            } catch (BizException e) {
                fail++;
                errors.add(new UserImportResultVO.ErrorItem(i + 2, username, e.getMessage()));
            }
        }
        return new UserImportResultVO(success, fail, errors);
    }

    @Override
    public void downloadUserTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=user_import_template.xlsx");
        EasyExcel.write(response.getOutputStream(), UserImportDTO.class)
                .sheet("用户导入模板").doWrite(Collections.emptyList());
    }

    private UserExportVO toExportVO(User user) {
        UserExportVO vo = new UserExportVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(switch (user.getGender() == null ? 0 : user.getGender()) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        });
        if (user.getDeptId() != null) {
            var dept = deptMapper.selectOneById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }
        vo.setPostName(user.getPostName());
        vo.setStatus(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
        vo.setCreateTime(user.getCreateTime() == null ? "" : user.getCreateTime().toString());
        return vo;
    }
}
