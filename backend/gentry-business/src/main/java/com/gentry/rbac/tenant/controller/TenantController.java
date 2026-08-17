package com.gentry.rbac.tenant.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.tenant.dto.*;
import com.gentry.rbac.tenant.service.TenantService;
import com.gentry.rbac.tenant.vo.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /** TENANT-001 租户列表查询 */
    @GetMapping
    @SaCheckPermission("system:tenant:list")
    public R<PageResult<TenantListVO>> list(@Valid TenantQueryDTO query) {
        return R.ok(tenantService.list(query));
    }

    /** TENANT-002 租户详情 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:tenant:list")
    public R<TenantDetailVO> detail(@PathVariable Long id) {
        return R.ok(tenantService.getDetail(id));
    }

    /** TENANT-003 新增租户 */
    @PostMapping
    @SaCheckPermission("system:tenant:add")
    @Log(module = "租户管理", type = "INSERT", title = "新增租户")
    public R<TenantCreateResultVO> create(@Valid @RequestBody TenantCreateDTO dto) {
        return R.ok(tenantService.create(dto));
    }

    /** TENANT-004 编辑租户 */
    @PutMapping("/{id}")
    @SaCheckPermission("system:tenant:edit")
    @Log(module = "租户管理", type = "UPDATE", title = "编辑租户")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateDTO dto) {
        tenantService.update(id, dto);
        return R.ok();
    }

    /** TENANT-005 删除租户 */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:tenant:remove")
    @Log(module = "租户管理", type = "DELETE", title = "删除租户")
    public R<Void> remove(@PathVariable Long id) {
        tenantService.remove(id);
        return R.ok();
    }

    /** TENANT-006 租户配置 */
    @PutMapping("/{id}/config")
    @SaCheckPermission("system:tenant:config")
    @Log(module = "租户管理", type = "UPDATE", title = "租户配置")
    public R<Void> config(@PathVariable Long id, @Valid @RequestBody TenantConfigDTO dto) {
        tenantService.updateConfig(id, dto);
        return R.ok();
    }

    /** TENANT-007 切换租户状态 */
    @PutMapping("/{id}/status")
    @SaCheckPermission("system:tenant:edit")
    @Log(module = "租户管理", type = "UPDATE", title = "切换租户状态")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody TenantStatusDTO dto) {
        tenantService.updateStatus(id, dto);
        return R.ok();
    }

    /** TENANT-009 租户选项列表（公开接口，登录页下拉框） */
    @GetMapping("/options")
    public R<List<TenantOptionVO>> options() {
        return R.ok(tenantService.listOptions());
    }
}
