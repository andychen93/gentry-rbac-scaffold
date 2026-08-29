package com.gentry.rbac.log.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.log.dto.*;
import com.gentry.rbac.log.service.LogService;
import com.gentry.rbac.log.vo.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /** LOG-001 操作日志列表 */
    @GetMapping("/api/v1/logs/operation")
    @SaCheckPermission("system:operlog:list")
    public R<PageResult<OperLogListVO>> listOperLogs(@Valid OperLogQueryDTO query) {
        return R.ok(logService.listOperLogs(query));
    }

    /** LOG-002 操作日志详情 */
    @GetMapping("/api/v1/logs/operation/{id}")
    @SaCheckPermission("system:operlog:detail")
    public R<OperLogDetailVO> getOperLogDetail(@PathVariable Long id) {
        return R.ok(logService.getOperLogDetail(id));
    }

    /** LOG-004 导出操作日志 */
    @GetMapping("/api/v1/logs/operation/export")
    @SaCheckPermission("system:operlog:export")
    @Log(module = "日志管理", type = "EXPORT", title = "导出操作日志")
    public ResponseEntity<byte[]> exportOperLogs(@Valid OperLogQueryDTO query) {
        return csv("oper_logs.csv", logService.exportOperLogs(query));
    }

    /** LOG-003 清理操作日志 */
    @DeleteMapping("/api/v1/logs/operation")
    @SaCheckPermission("system:operlog:remove")
    @Log(module = "日志管理", type = "DELETE", title = "清理操作日志")
    public R<Integer> cleanOperLogs(@Valid @RequestBody LogCleanDTO dto) {
        return R.ok(logService.cleanOperLogs(dto.getBeforeDays()));
    }

    /** LOG-005 登录日志列表 */
    @GetMapping("/api/v1/logs/login")
    @SaCheckPermission("system:loginlog:list")
    public R<PageResult<LoginLogListVO>> listLoginLogs(@Valid LoginLogQueryDTO query) {
        return R.ok(logService.listLoginLogs(query));
    }

    /** LOG-006 登录日志详情 */
    @GetMapping("/api/v1/logs/login/{id}")
    @SaCheckPermission("system:loginlog:detail")
    public R<LoginLogDetailVO> getLoginLogDetail(@PathVariable Long id) {
        return R.ok(logService.getLoginLogDetail(id));
    }

    /** LOG-008 导出登录日志 */
    @GetMapping("/api/v1/logs/login/export")
    @SaCheckPermission("system:loginlog:export")
    @Log(module = "日志管理", type = "EXPORT", title = "导出登录日志")
    public ResponseEntity<byte[]> exportLoginLogs(@Valid LoginLogQueryDTO query) {
        return csv("login_logs.csv", logService.exportLoginLogs(query));
    }

    /** LOG-007 清理登录日志 */
    @DeleteMapping("/api/v1/logs/login")
    @SaCheckPermission("system:loginlog:remove")
    @Log(module = "日志管理", type = "DELETE", title = "清理登录日志")
    public R<Integer> cleanLoginLogs(@Valid @RequestBody LogCleanDTO dto) {
        return R.ok(logService.cleanLoginLogs(dto.getBeforeDays()));
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }
}
