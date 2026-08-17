package com.gentry.monitor.redis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.monitor.redis.service.RedisKeyService;
import com.gentry.monitor.redis.service.RedisMonitorService;
import com.gentry.monitor.redis.vo.RedisKeyDefineVO;
import com.gentry.monitor.redis.vo.RedisKeyVO;
import com.gentry.monitor.redis.vo.RedisMonitorVO;
import com.gentry.monitor.redis.vo.RedisSlowLogVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Redis 监控接口。
 */
@RestController
@RequestMapping("/api/v1/monitor/redis")
public class RedisMonitorController {

    private final RedisMonitorService monitorService;
    private final RedisKeyService keyService;

    public RedisMonitorController(RedisMonitorService monitorService, RedisKeyService keyService) {
        this.monitorService = monitorService;
        this.keyService = keyService;
    }

    /** 获取 Redis 监控总览（INFO + DBSIZE + commandstats） */
    @GetMapping("/info")
    @SaCheckPermission("monitor:redis:info")
    public R<RedisMonitorVO> getMonitorInfo() {
        return R.ok(monitorService.getMonitorInfo());
    }

    /** 获取预定义 Key 模板列表 */
    @GetMapping("/key-defines")
    @SaCheckPermission("monitor:redis:info")
    public R<List<RedisKeyDefineVO>> getKeyDefines() {
        return R.ok(monitorService.getKeyDefines());
    }

    /** 按模式扫描 Key */
    @GetMapping("/keys")
    @SaCheckPermission("monitor:redis:key:list")
    public R<PageResult<RedisKeyVO>> scanKeys(
            @RequestParam(defaultValue = "*") String pattern,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(keyService.scanKeys(pattern, pageNum, pageSize));
    }

    /** 获取指定 Key 的值详情 */
    @GetMapping("/keys/{key}/value")
    @SaCheckPermission("monitor:redis:key:query")
    public R<RedisKeyVO> getKeyValue(@PathVariable String key) {
        return R.ok(keyService.getKeyValue(key));
    }

    /** 删除指定 Key */
    @DeleteMapping("/keys/{key}")
    @SaCheckPermission("monitor:redis:key:delete")
    public R<Void> deleteKey(@PathVariable String key) {
        keyService.deleteKey(key);
        return R.ok();
    }

    /** 获取慢查询日志（默认 20 条，最大 200 条） */
    @GetMapping("/slowlog")
    @SaCheckPermission("monitor:redis:info")
    public R<List<RedisSlowLogVO>> getSlowLog(@RequestParam(defaultValue = "20") int limit) {
        return R.ok(monitorService.getSlowLog(limit));
    }

    /** 清空慢查询日志 */
    @DeleteMapping("/slowlog")
    @SaCheckPermission("monitor:redis:slowlog:reset")
    public R<Void> resetSlowLog() {
        monitorService.resetSlowLog();
        return R.ok();
    }
}
