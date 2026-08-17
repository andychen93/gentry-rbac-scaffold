package com.gentry.rbac.menu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.R;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.menu.dto.MenuCreateDTO;
import com.gentry.rbac.menu.dto.MenuQueryDTO;
import com.gentry.rbac.menu.dto.MenuUpdateDTO;
import com.gentry.rbac.menu.service.MenuService;
import com.gentry.rbac.menu.vo.MenuTreeVO;
import com.gentry.rbac.menu.vo.MenuVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** API-001 菜单树查询 */
    @GetMapping({"", "/tree"})
    @SaCheckPermission("system:menu:list")
    public R<List<MenuTreeVO>> tree(MenuQueryDTO query) {
        return R.ok(menuService.tree(query));
    }

    /** API-002 菜单详情 */
    @GetMapping("/{id}")
    @SaCheckPermission("system:menu:list")
    public R<MenuTreeVO> detail(@PathVariable Long id) {
        return R.ok(menuService.getDetail(id));
    }

    /** API-003 新增菜单 */
    @PostMapping
    @SaCheckPermission("system:menu:add")
    @Log(module = "菜单管理", type = "INSERT", title = "新增菜单")
    public R<MenuVO> create(@Valid @RequestBody MenuCreateDTO dto) {
        return R.ok(menuService.create(dto));
    }

    /** API-004 编辑菜单 */
    @PutMapping("/{id}")
    @SaCheckPermission("system:menu:edit")
    @Log(module = "菜单管理", type = "UPDATE", title = "编辑菜单")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        menuService.update(id, dto);
        return R.ok();
    }

    /** API-005 删除菜单 */
    @DeleteMapping("/{id}")
    @SaCheckPermission("system:menu:remove")
    @Log(module = "菜单管理", type = "DELETE", title = "删除菜单")
    public R<Void> remove(@PathVariable Long id) {
        menuService.remove(id);
        return R.ok();
    }
}
