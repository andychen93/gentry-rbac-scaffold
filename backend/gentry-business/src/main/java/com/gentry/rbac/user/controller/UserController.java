package com.gentry.rbac.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.core.web.RepeatSubmit;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.user.dto.*;
import com.gentry.rbac.user.service.UserService;
import com.gentry.rbac.user.vo.UserDetailVO;
import com.gentry.rbac.user.vo.UserImportResultVO;
import com.gentry.rbac.user.vo.UserListVO;
import com.gentry.rbac.user.vo.UserOptionVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** USER-001 用户列表查询 */
    @GetMapping
    @SaCheckPermission("system:user:list")
    public R<PageResult<UserListVO>> list(@Valid UserQueryDTO query) {
        return R.ok(userService.list(query));
    }

    /**
     * USER-014 用户下拉选项（租户内启用用户）。
     *
     * <p>供「角色管理 → 绑定用户」穿梭框取候选，与 {@code GET /roles/options} 对称。
     * 用 {@code system:role:assignUser} 或 {@code system:user:list} 任一即可访问：
     * 只有绑定用户权限的角色管理员也要能拉到候选列表。</p>
     */
    @GetMapping("/options")
    @SaCheckPermission(value = {"system:user:list", "system:role:assignUser"}, mode = SaMode.OR)
    public R<List<UserOptionVO>> options() {
        return R.ok(userService.listOptions());
    }

    /** USER-002 用户详情 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:user:list")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return R.ok(userService.getDetail(id));
    }

    /** USER-003 新增用户（防重复提交：3 秒内禁止重复） */
    @PostMapping
    @SaCheckPermission("system:user:add")
    @RepeatSubmit(interval = 3)
    @Log(module = "用户管理", type = "INSERT", title = "新增用户")
    public R<UserDetailVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.create(dto));
    }

    /** USER-004 编辑用户 */
    @PutMapping("/{id}")
    @SaCheckPermission("system:user:edit")
    @Log(module = "用户管理", type = "UPDATE", title = "编辑用户")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return R.ok();
    }

    /** USER-005 删除用户 */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:user:remove")
    @Log(module = "用户管理", type = "DELETE", title = "删除用户")
    public R<Void> remove(@PathVariable Long id) {
        userService.remove(id);
        return R.ok();
    }

    /** USER-006 重置密码 */
    @PutMapping("/{id}/password/reset")
    @SaCheckPermission("system:user:resetPwd")
    @Log(module = "用户管理", type = "UPDATE", title = "重置密码")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserPasswordResetDTO dto) {
        userService.resetPassword(id, dto);
        return R.ok();
    }

    /** USER-007 分配角色 */
    @PutMapping("/{id}/roles")
    @SaCheckPermission("system:user:assignRole")
    @Log(module = "用户管理", type = "UPDATE", title = "分配角色")
    public R<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignDTO dto) {
        userService.assignRoles(id, dto);
        return R.ok();
    }

    /** USER-008 切换用户状态 */
    @PutMapping("/{id}/status")
    @SaCheckPermission("system:user:edit")
    @Log(module = "用户管理", type = "UPDATE", title = "切换用户状态")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userService.updateStatus(id, dto);
        return R.ok();
    }

    /** USER-009 导出用户（Excel，按当前查询条件） */
    @GetMapping("/export")
    @SaCheckPermission("system:user:export")
    @Log(module = "用户管理", type = "EXPORT", title = "导出用户")
    public void export(@Valid UserQueryDTO query, HttpServletResponse response) throws IOException {
        userService.exportUsers(query, response);
    }

    /** USER-010 导入用户（Excel） */
    @PostMapping("/import")
    @SaCheckPermission("system:user:import")
    @RepeatSubmit(interval = 5)
    @Log(module = "用户管理", type = "IMPORT", title = "导入用户")
    public R<UserImportResultVO> importUsers(@RequestParam("file") MultipartFile file) {
        return R.ok(userService.importUsers(file));
    }

    /** USER-011 下载导入模板 */
    @GetMapping("/import/template")
    @SaCheckPermission("system:user:import")
    public void importTemplate(HttpServletResponse response) throws IOException {
        userService.downloadUserTemplate(response);
    }
}
