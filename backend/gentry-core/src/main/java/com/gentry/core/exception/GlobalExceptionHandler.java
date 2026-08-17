package com.gentry.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 约束名 -> 友好提示 映射 */
    private static final Map<String, String> CONSTRAINT_MAP = Map.ofEntries(
            Map.entry("uk_user_username", "用户名已存在"),
            Map.entry("uk_tenant_code", "租户编码已存在"),
            Map.entry("uk_role_code", "角色编码已存在"),
            Map.entry("uk_dept_name", "部门名称已存在"),
            Map.entry("uk_dict_type", "字典类型已存在"),
            Map.entry("uk_device_sn", "设备序列号已存在"),
            Map.entry("uk_vehicle_plate", "车牌号已存在")
    );
    private static final String DEFAULT_DUPLICATE_MSG = "数据已存在，请检查唯一性字段";
    private static final Pattern PG_PATTERN = Pattern.compile("constraint \"(\\w+)\"");
    private static final Pattern MYSQL_PATTERN = Pattern.compile("for key '(\\w+)'");

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidationException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        String message = String.join("; ", errors);
        log.warn("参数校验失败: {}", message);
        return R.fail(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public R<?> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return R.fail(ErrorCode.PARAM_ERROR, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("HTTP消息不可读: {}", e.getMessage());
        return R.fail(ErrorCode.HTTP_MESSAGE_NOT_READABLE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数 '" + e.getName() + "' 类型不匹配";
        log.warn("参数类型不匹配: {}", message);
        return R.fail(ErrorCode.PARAM_TYPE_MISMATCH, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不允许: {}", e.getMethod());
        return R.fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // ==================== Sa-Token 认证鉴权异常 ====================

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<?> handleNotLoginException(NotLoginException e) {
        String type = e.getType();
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            log.warn("Token已过期: {}", e.getMessage());
            return R.fail(ErrorCode.TOKEN_EXPIRED);
        }
        if (NotLoginException.BE_REPLACED.equals(type) || NotLoginException.KICK_OUT.equals(type)) {
            log.warn("账号已在其他设备登录: {}", e.getMessage());
            return R.fail(ErrorCode.TOKEN_INVALID, "账号已在其他设备登录");
        }
        log.warn("未登录或Token无效: type={}, msg={}", type, e.getMessage());
        return R.fail(ErrorCode.TOKEN_INVALID);
    }

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotRoleException(NotRoleException e) {
        log.warn("角色不足: role={}", e.getRole());
        return R.fail(ErrorCode.PERMISSION_DENIED, "缺少角色: " + e.getRole());
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限不足: permission={}", e.getPermission());
        return R.fail(ErrorCode.PERMISSION_DENIED, "缺少权限: " + e.getPermission());
    }

    // ==================== 数据库异常 ====================

    @ExceptionHandler(DuplicateKeyException.class)
    public R<?> handleDuplicateKeyException(DuplicateKeyException e) {
        String message = resolveDuplicateMessage(e.getMessage());
        log.warn("数据重复: {}", message);
        return R.fail(ErrorCode.DATA_EXISTS, message);
    }

    // ==================== 静态资源未找到 ====================

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNoResource(NoResourceFoundException e) {
        return R.fail(ErrorCode.PARAM_ERROR, "接口不存在: " + e.getResourcePath());
    }

    // ==================== 兜底异常 ====================

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return R.fail(ErrorCode.SYSTEM_ERROR);
    }

    // ==================== 私有方法 ====================

    private String resolveDuplicateMessage(String msg) {
        if (msg == null || msg.isEmpty()) return DEFAULT_DUPLICATE_MSG;
        Matcher pgMatcher = PG_PATTERN.matcher(msg);
        if (pgMatcher.find()) {
            return CONSTRAINT_MAP.getOrDefault(pgMatcher.group(1), DEFAULT_DUPLICATE_MSG);
        }
        Matcher mysqlMatcher = MYSQL_PATTERN.matcher(msg);
        if (mysqlMatcher.find()) {
            return CONSTRAINT_MAP.getOrDefault(mysqlMatcher.group(1), DEFAULT_DUPLICATE_MSG);
        }
        return DEFAULT_DUPLICATE_MSG;
    }
}
