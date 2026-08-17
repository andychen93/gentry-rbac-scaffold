package com.gentry.rbac.dept.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.R;
import com.gentry.rbac.dept.dto.DeptCreateDTO;
import com.gentry.rbac.dept.dto.DeptQueryDTO;
import com.gentry.rbac.dept.dto.DeptUpdateDTO;
import com.gentry.rbac.dept.service.DeptService;
import com.gentry.rbac.dept.vo.DeptTreeVO;
import com.gentry.rbac.dept.vo.DeptVO;
import com.gentry.rbac.log.annotation.Log;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/depts")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /** API-001 部门树查询 */
    @GetMapping({"", "/tree"})
    @SaCheckPermission("system:dept:list")
    public R<List<DeptTreeVO>> tree(DeptQueryDTO query) {
        return R.ok(deptService.tree(query));
    }

    /** API-002 部门详情 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:dept:list")
    public R<DeptTreeVO> detail(@PathVariable Long id) {
        return R.ok(deptService.getDetail(id));
    }

    /** API-003 新增部门 */
    @PostMapping
    @SaCheckPermission("system:dept:add")
    @Log(module = "部门管理", type = "INSERT", title = "新增部门")
    public R<DeptVO> create(@Valid @RequestBody DeptCreateDTO dto) {
        return R.ok(deptService.create(dto));
    }

    /** API-004 编辑部门 */
    @PutMapping("/{id}")
    @SaCheckPermission("system:dept:edit")
    @Log(module = "部门管理", type = "UPDATE", title = "编辑部门")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        deptService.update(id, dto);
        return R.ok();
    }

    /** API-005 删除部门 */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dept:remove")
    @Log(module = "部门管理", type = "DELETE", title = "删除部门")
    public R<Void> remove(@PathVariable Long id) {
        deptService.remove(id);
        return R.ok();
    }
}
