package com.gentry.rbac.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.rbac.config.dto.ConfigCreateDTO;
import com.gentry.rbac.config.dto.ConfigQueryDTO;
import com.gentry.rbac.config.dto.ConfigUpdateDTO;
import com.gentry.rbac.config.service.ConfigService;
import com.gentry.rbac.config.vo.ConfigVO;
import com.gentry.rbac.log.annotation.Log;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数配置 Controller
 */
@RestController
@RequestMapping("/api/v1/configs")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @SaCheckPermission("system:config:list")
    public R<PageResult<ConfigVO>> list(@Valid ConfigQueryDTO query) {
        return R.ok(configService.list(query));
    }

    @PostMapping
    @SaCheckPermission("system:config:add")
    @Log(module = "参数配置", type = "INSERT", title = "新增参数")
    public R<ConfigVO> create(@Valid @RequestBody ConfigCreateDTO dto) {
        return R.ok(configService.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:config:edit")
    @Log(module = "参数配置", type = "UPDATE", title = "编辑参数")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ConfigUpdateDTO dto) {
        configService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:config:remove")
    @Log(module = "参数配置", type = "DELETE", title = "删除参数")
    public R<Void> remove(@PathVariable Long id) {
        configService.remove(id);
        return R.ok();
    }

    @DeleteMapping("/cache")
    @SaCheckPermission("system:config:refresh")
    @Log(module = "参数配置", type = "UPDATE", title = "刷新参数缓存")
    public R<Void> refreshCache() {
        configService.refreshCache();
        return R.ok();
    }
}
