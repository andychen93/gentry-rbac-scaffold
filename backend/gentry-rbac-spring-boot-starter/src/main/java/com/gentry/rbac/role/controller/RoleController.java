package com.gentry.rbac.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.role.dto.*;
import com.gentry.rbac.role.service.RoleService;
import com.gentry.rbac.role.vo.RoleDetailVO;
import com.gentry.rbac.role.vo.RoleListVO;
import com.gentry.rbac.role.vo.RoleOptionVO;
import com.gentry.rbac.role.vo.RoleVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /** API-001 角色列表查询 */
    @GetMapping
    @SaCheckPermission("system:role:list")
    public R<PageResult<RoleListVO>> list(@Valid RoleQueryDTO query) {
        return R.ok(roleService.list(query));
    }

    /** API-010 角色下拉选项（启用角色，供用户分配等场景） */
    @GetMapping("/options")
    @SaCheckPermission("system:role:list")
    public R<List<RoleOptionVO>> options() {
        return R.ok(roleService.listOptions());
    }

    /** API-002 角色详情 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:role:list")
    public R<RoleDetailVO> detail(@PathVariable Long id) {
        return R.ok(roleService.getDetail(id));
    }

    /** API-003 新增角色 */
    @PostMapping
    @SaCheckPermission("system:role:add")
    @Log(module = "角色管理", type = "INSERT", title = "新增角色")
    public R<RoleVO> create(@Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(roleService.create(dto));
    }

    /** API-004 编辑角色 */
    @PutMapping("/{id}")
    @SaCheckPermission("system:role:edit")
    @Log(module = "角色管理", type = "UPDATE", title = "编辑角色")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.update(id, dto);
        return R.ok();
    }

    /** API-005 删除角色 */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:role:remove")
    @Log(module = "角色管理", type = "DELETE", title = "删除角色")
    public R<Void> remove(@PathVariable Long id) {
        roleService.remove(id);
        return R.ok();
    }

    /** API-006 分配菜单权限 */
    @PutMapping("/{id}/menus")
    @SaCheckPermission("system:role:assignMenu")
    @Log(module = "角色管理", type = "UPDATE", title = "分配菜单权限")
    public R<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuAssignDTO dto) {
        roleService.assignMenus(id, dto);
        return R.ok();
    }

    /** API-007 设置数据权限 */
    @PutMapping("/{id}/data-scope")
    @SaCheckPermission("system:role:assignDataScope")
    @Log(module = "角色管理", type = "UPDATE", title = "设置数据权限")
    public R<Void> updateDataScope(@PathVariable Long id, @Valid @RequestBody RoleDataScopeDTO dto) {
        roleService.updateDataScope(id, dto);
        return R.ok();
    }

    /** API-009 切换角色状态 */
    @PutMapping("/{id}/status")
    @SaCheckPermission("system:role:edit")
    @Log(module = "角色管理", type = "UPDATE", title = "切换角色状态")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody RoleStatusDTO dto) {
        roleService.updateStatus(id, dto);
        return R.ok();
    }

    /** API-008 查看关联用户 */
    @GetMapping("/{id}/users")
    @SaCheckPermission("system:role:list")
    public R<List<Long>> listUserIds(@PathVariable Long id) {
        return R.ok(roleService.listUserIdsByRoleId(id));
    }

    /** API-011 绑定用户（全量覆盖） */
    @PutMapping("/{id}/users")
    @SaCheckPermission("system:role:assignUser")
    @Log(module = "角色管理", type = "UPDATE", title = "绑定用户")
    public R<Void> assignUsers(@PathVariable Long id, @Valid @RequestBody RoleUserAssignDTO dto) {
        roleService.assignUsers(id, dto);
        return R.ok();
    }
}
