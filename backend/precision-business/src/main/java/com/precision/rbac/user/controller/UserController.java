package com.precision.rbac.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.precision.core.common.PageResult;
import com.precision.core.common.R;
import com.precision.core.web.RepeatSubmit;
import com.precision.rbac.log.annotation.Log;
import com.precision.rbac.user.dto.*;
import com.precision.rbac.user.service.UserService;
import com.precision.rbac.user.vo.UserDetailVO;
import com.precision.rbac.user.vo.UserListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
