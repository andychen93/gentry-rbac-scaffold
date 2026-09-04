package com.gentry.rbac.dict.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.rbac.dict.dto.*;
import com.gentry.rbac.dict.service.DictService;
import com.gentry.rbac.dict.vo.*;
import com.gentry.rbac.log.annotation.Log;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /** DICT-001 字典类型列表 */
    @GetMapping("/types")
    @SaCheckPermission("system:dict:list")
    public R<PageResult<DictTypeListVO>> listTypes(@Valid DictTypeQueryDTO query) {
        return R.ok(dictService.listTypes(query));
    }

    /** DICT-002 新增字典类型 */
    @PostMapping("/types")
    @SaCheckPermission("system:dict:add")
    @Log(module = "字典管理", type = "INSERT", title = "新增字典类型")
    public R<DictTypeVO> createType(@Valid @RequestBody DictTypeCreateDTO dto) {
        return R.ok(dictService.createType(dto));
    }

    /** DICT-003 编辑字典类型 */
    @PutMapping("/types/{id}")
    @SaCheckPermission("system:dict:edit")
    @Log(module = "字典管理", type = "UPDATE", title = "编辑字典类型")
    public R<Void> updateType(@PathVariable Long id, @Valid @RequestBody DictTypeUpdateDTO dto) {
        dictService.updateType(id, dto);
        return R.ok();
    }

    /** DICT-004 删除字典类型 */
    @DeleteMapping("/types/{id}")
    @SaCheckPermission("system:dict:remove")
    @Log(module = "字典管理", type = "DELETE", title = "删除字典类型")
    public R<Void> removeType(@PathVariable Long id) {
        dictService.removeType(id);
        return R.ok();
    }

    /** DICT-005 查询字典数据 */
    @GetMapping("/types/{dictType}/data")
    @SaCheckPermission("system:dict:list")
    public R<List<DictDataVO>> listData(@PathVariable String dictType) {
        return R.ok(dictService.listDataByType(dictType));
    }

    /** DICT-006 新增字典数据 */
    @PostMapping("/types/{dictType}/data")
    @SaCheckPermission("system:dict:add")
    @Log(module = "字典管理", type = "INSERT", title = "新增字典数据")
    public R<DictDataVO> createData(@PathVariable String dictType, @Valid @RequestBody DictDataCreateDTO dto) {
        return R.ok(dictService.createData(dictType, dto));
    }

    /** DICT-007 编辑字典数据 */
    @PutMapping("/data/{id}")
    @SaCheckPermission("system:dict:edit")
    @Log(module = "字典管理", type = "UPDATE", title = "编辑字典数据")
    public R<Void> updateData(@PathVariable Long id, @Valid @RequestBody DictDataUpdateDTO dto) {
        dictService.updateData(id, dto);
        return R.ok();
    }

    /** DICT-008 删除字典数据 */
    @DeleteMapping("/data/{id}")
    @SaCheckPermission("system:dict:remove")
    @Log(module = "字典管理", type = "DELETE", title = "删除字典数据")
    public R<Void> removeData(@PathVariable Long id) {
        dictService.removeData(id);
        return R.ok();
    }

    /** DICT-009 刷新字典缓存 */
    @DeleteMapping("/cache")
    @SaCheckPermission("system:dict:edit")
    public R<Void> refreshCache() {
        dictService.refreshCache();
        return R.ok();
    }
}
