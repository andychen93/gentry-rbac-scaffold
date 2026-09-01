package com.gentry.rbac.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.i18n.I18nProperties;
import com.gentry.core.i18n.I18nUtil;
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
import com.gentry.core.i18n.LocalizedCodeResolver;
import com.gentry.rbac.dict.service.DictService;
import com.gentry.rbac.dict.vo.DictDataVO;
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
    /** 职务字典类型。码存进 sys_user.post_name，译文走 export.user.post.{code} */
    private static final String POST_DICT_TYPE = "sys_user_post";
    /**
     * 性别码 → 译文 key。
     *
     * <p>性别不是字典驱动的（0/1/2 硬编码在业务里，{@code User.gender} 就是 Integer），
     * 所以单列一张表，而职务的码清单从 {@link #POST_DICT_TYPE} 字典查。</p>
     */
    private static final Map<String, String> GENDER_KEYS = Map.of(
            "0", "export.user.gender.unknown",
            "1", "export.user.gender.male",
            "2", "export.user.gender.female");

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;
    private final DeptService deptService;
    private final PasswordEncoder passwordEncoder;
    private final I18nProperties i18nProperties;
    private final I18nUtil i18nUtil;
    /** 职务码清单的来源：sys_user_post 字典（带 Caffeine 缓存，导入时只查一次） */
    private final DictService dictService;
    private final LocalizedCodeResolver codeResolver;

    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper,
                           RoleMapper roleMapper, DeptMapper deptMapper,
                           DeptService deptService,
                           PasswordEncoder passwordEncoder,
                           I18nProperties i18nProperties,
                           I18nUtil i18nUtil,
                           DictService dictService,
                           LocalizedCodeResolver codeResolver) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
        this.deptService = deptService;
        this.passwordEncoder = passwordEncoder;
        this.i18nProperties = i18nProperties;
        this.i18nUtil = i18nUtil;
        this.dictService = dictService;
        this.codeResolver = codeResolver;
    }

    @Override
    @Transactional
    public UserDetailVO create(UserCreateDTO dto) {
        // 校验用户名不能是 admin
        if (ADMIN_USERNAME.equals(dto.getUsername())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.username.reserved");
        }

        // 校验用户名唯一
        if (userMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        // 校验手机号唯一
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.countByPhone(dto.getPhone(), null) > 0) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.user.phone.in.use");
            }
        }

        // 构建 User 实体（ID 由 @Id 注解自动生成，createBy 由 AutoFillHandler 自动填充）
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
        User existing = getExistingUser(id);

        // 校验手机号唯一（排除自身）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.countByPhone(dto.getPhone(), id) > 0) {
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
        List<Long> deptIds = null;

        // 如果指定了部门，查询该部门及子部门
        if (query.getDeptId() != null) {
            deptIds = deptService.getChildDeptIds(query.getDeptId());
        }

        long total = userMapper.selectCount(query, deptIds);
        List<User> users = Collections.emptyList();
        if (total > 0) {
            users = userMapper.selectList(query, deptIds);
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
        getExistingUser(userId);

        // 手机号唯一校验（排除自身）
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (userMapper.countByPhone(dto.getPhone(), userId) > 0) {
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

    @Override
    @Transactional
    public void updateMyLanguage(String language) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        // 白名单校验放在这里而不是 DTO 的 @Pattern：注解常量读不到配置，
        // 语言清单的单一真源是 gentry.i18n.supported-locales
        if (!i18nProperties.getSupportedLocales().contains(language)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.language.unsupported", language);
        }
        int n = userMapper.updateLanguage(userId, language);
        if (n == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        /*
         * 同步 Sa-Token Session。不同步的后果很隐蔽：语言落库了，但 Session 里还是旧值，
         * 于是「导出 Excel / 定时通知」这些后端场景仍按旧语言走，直到用户重新登录 ——
         * 而那两个场景正是当初选用户级偏好（而非纯请求头）的全部理由。
         */
        StpUtil.getSession().set("language", language);
        UserContext.setLanguage(language);
        log.info("User {} switched language to {}", userId, language);
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
        List<UserRole> userRoles = roleIds.stream().map(roleId -> {
            UserRole ur = new UserRole(userId, roleId);
            ur.setId(IdGenerator.nextId());
            return ur;
        }).collect(Collectors.toList());
        userRoleMapper.batchInsert(userRoles);
    }

    @Override
    public List<UserOptionVO> listOptions() {
        return userMapper.selectOptions();
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
        List<Long> deptIds = query.getDeptId() != null ? deptService.getChildDeptIds(query.getDeptId()) : null;
        List<User> users = userMapper.selectList(query, deptIds);
        // 职务码 → 展示名的映射只算一次，避免每行都查一遍字典
        Map<String, String> postKeys = postCodeToMessageKey();
        List<UserExportVO> rows = users.stream()
                .map(u -> toExportVO(u, postKeys))
                .collect(Collectors.toList());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");
        // 表头随 locale 现场构建；.head(...) 会覆盖类上的 @ExcelProperty，
        // 所以 UserExportVO 只留 index 不留文案
        EasyExcel.write(response.getOutputStream(), UserExportVO.class)
                .head(buildHead(UserExportVO.HEAD_KEYS))
                .sheet(i18nUtil.getMessage("export.user.sheet", "用户列表"))
                .doWrite(rows);
    }

    @Override
    public UserImportResultVO importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.import.file.empty");
        }
        List<UserImportDTO> rows;
        try (InputStream is = file.getInputStream()) {
            // headRowNumber(1)：跳过表头行，之后按 @ExcelProperty(index) 对列，
            // 与表头是中文还是英文无关 —— 这是「英文导出的文件能导回来」的关键
            rows = EasyExcel.read(is).head(UserImportDTO.class).headRowNumber(1)
                    .sheet().doReadSync();
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.user.import.file.read.failed");
        }
        int success = 0, fail = 0;
        List<UserImportResultVO.ErrorItem> errors = new ArrayList<>();
        // 同上，整批只算一次
        Map<String, String> postKeys = postCodeToMessageKey();
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
                /*
                 * 性别与职务都「码或任一语言的 label 都认」，见 LocalizedCodeResolver。
                 * 认不出时按未填处理（性别退 0、职务留空）而不是整行报错 ——
                 * 这两列都是可选信息，为一个拼错的职务名丢掉整行用户不划算。
                 */
                String genderCode = codeResolver.resolve(GENDER_KEYS, row.getGender());
                dto.setGender(genderCode != null ? Integer.valueOf(genderCode) : 0);
                dto.setPostName(codeResolver.resolve(postKeys, row.getPostName()));
                dto.setPassword(DEFAULT_IMPORT_PASSWORD);
                dto.setStatus(1);
                create(dto);
                success++;
            } catch (BizException e) {
                fail++;
                // e.getMessage() 现在是 i18n key（BizException 的 super 存 key 便于日志溯源），
                // 这里是给用户看的，必须按请求 locale 翻译，否则会把 error.xxx 露到界面上
                String localized = i18nUtil.getMessage(
                        e.resolveI18nKey(), e.getFallbackMessage(), e.getArgs());
                errors.add(new UserImportResultVO.ErrorItem(i + 2, username, localized));
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
                .head(buildHead(UserImportDTO.HEAD_KEYS))
                .sheet(i18nUtil.getMessage("export.user.template.sheet", "用户导入模板"))
                .doWrite(Collections.emptyList());
    }

    /** 按当前 locale 现场构建 Excel 表头。EasyExcel 的 head 是 List<List<String>>，每列一个 List */
    private List<List<String>> buildHead(List<String> headKeys) {
        return headKeys.stream()
                .map(k -> List.of(i18nUtil.getMessage(k, k)))
                .collect(Collectors.toList());
    }

    /**
     * 职务码 → 译文 i18n key。
     *
     * <p>码清单取自 {@code sys_user_post} 字典（唯一真源，带缓存），译文取自
     * {@code export.user.post.{code}}。<b>为什么译文要在后端再放一份</b>：Excel 是后端
     * 生成的，而字典 label 的译文按既有决策放在前端语言包（{@code locales/{lang}/dict.json}），
     * 后端拿不到。两份由 {@code frontend/src/locales/exportDictParity.test.ts} 逐条对齐，
     * 把「可能漂移」变成「构造上不可能漂移」。</p>
     */
    private Map<String, String> postCodeToMessageKey() {
        Map<String, String> map = new LinkedHashMap<>();
        for (DictDataVO d : dictService.listDataByType(POST_DICT_TYPE)) {
            map.put(d.getDictValue(), "export.user.post." + d.getDictValue());
        }
        return map;
    }

    private UserExportVO toExportVO(User user, Map<String, String> postKeys) {
        UserExportVO vo = new UserExportVO();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(switch (user.getGender() == null ? 0 : user.getGender()) {
            case 1 -> i18nUtil.getMessage("export.user.gender.male", "男");
            case 2 -> i18nUtil.getMessage("export.user.gender.female", "女");
            default -> i18nUtil.getMessage("export.user.gender.unknown", "未知");
        });
        if (user.getDeptId() != null) {
            var dept = deptMapper.selectOneById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getName());
        }
        // 库里存的是字典码（V13 起），导出给人看的是当前语言的展示名
        String postCode = user.getPostName();
        vo.setPostName(postCode == null ? "" : i18nUtil.getMessage(
                postKeys.getOrDefault(postCode, "export.user.post." + postCode), postCode));
        vo.setStatus(user.getStatus() != null && user.getStatus() == 1
                ? i18nUtil.getMessage("export.user.status.enabled", "启用")
                : i18nUtil.getMessage("export.user.status.disabled", "禁用"));
        vo.setCreateTime(user.getCreateTime() == null ? "" : user.getCreateTime().toString());
        return vo;
    }
}
