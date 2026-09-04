package com.gentry.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.R;
import com.gentry.core.i18n.I18nUtil;
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

    /**
     * 唯一约束名 -> ErrorCode。命中后走 ErrorCode 的 i18n key，不再写死中文。
     *
     * <p>此前混入的 {@code uk_device_sn} / {@code uk_vehicle_plate}（设备序列号、车牌号）
     * 是从高精度定位平台拷来的死条目 —— 库里没有这两张表，RBAC 脚手架里也不该出现
     * 业务领域概念，已清除。</p>
     */
    private static final Map<String, ErrorCode> CONSTRAINT_MAP = Map.ofEntries(
            Map.entry("uk_user_username", ErrorCode.USERNAME_EXISTS),
            Map.entry("uk_user_phone", ErrorCode.PHONE_EXISTS),
            Map.entry("uk_role_code", ErrorCode.ROLE_CODE_EXISTS),
            Map.entry("uk_dept_name", ErrorCode.DEPT_NAME_EXISTS),
            Map.entry("uk_dict_type", ErrorCode.DICT_TYPE_EXISTS)
    );
    private static final Pattern PG_PATTERN = Pattern.compile("constraint \"(\\w+)\"");
    private static final Pattern MYSQL_PATTERN = Pattern.compile("for key '(\\w+)'");

    private final I18nUtil i18nUtil;

    public GlobalExceptionHandler(I18nUtil i18nUtil) {
        this.i18nUtil = i18nUtil;
    }

    /** 本地化 ErrorCode 自带的消息，找不到译文时回退枚举里的中文 */
    private String localize(ErrorCode ec) {
        return i18nUtil.getMessage(ec.i18nKey(), ec.getMessage());
    }

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e) {
        // key 来源：显式 messageKey 优先，否则由 ErrorCode 枚举名派生
        String key = e.resolveI18nKey();
        String message = i18nUtil.getMessage(key, e.getFallbackMessage(), e.getArgs());
        // 日志记 key 而非译文：便于 grep，且不受阅读者语言影响
        log.warn("Business exception: code={}, key={}", e.getCode(), key);
        return R.fail(e.getCode(), message);
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidationException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        String message = String.join("; ", errors);
        log.warn("Validation failed: {}", message);
        // 字段提示已由 LocalValidatorFactoryBean 接 MessageSource 本地化（见批次 3）
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public R<?> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : localize(ErrorCode.PARAM_ERROR);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("HTTP message not readable: {}", e.getMessage());
        return R.fail(ErrorCode.HTTP_MESSAGE_NOT_READABLE.getCode(),
                localize(ErrorCode.HTTP_MESSAGE_NOT_READABLE));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = i18nUtil.getMessage("error.param.type.mismatch.detail",
                "参数 '" + e.getName() + "' 类型不匹配", e.getName());
        log.warn("Parameter type mismatch: name={}", e.getName());
        return R.fail(ErrorCode.PARAM_TYPE_MISMATCH.getCode(), message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("Request method not allowed: {}", e.getMethod());
        return R.fail(ErrorCode.METHOD_NOT_ALLOWED.getCode(), localize(ErrorCode.METHOD_NOT_ALLOWED));
    }

    // ==================== Sa-Token 认证鉴权异常 ====================

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<?> handleNotLoginException(NotLoginException e) {
        String type = e.getType();
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            log.warn("Token expired: {}", e.getMessage());
            return R.fail(ErrorCode.TOKEN_EXPIRED.getCode(), localize(ErrorCode.TOKEN_EXPIRED));
        }
        if (NotLoginException.BE_REPLACED.equals(type) || NotLoginException.KICK_OUT.equals(type)) {
            log.warn("Account signed in on another device: {}", e.getMessage());
            return R.fail(ErrorCode.TOKEN_INVALID.getCode(),
                    i18nUtil.getMessage("error.token.replaced", "账号已在其他设备登录"));
        }
        log.warn("Not logged in or invalid token: type={}, msg={}", type, e.getMessage());
        return R.fail(ErrorCode.TOKEN_INVALID.getCode(), localize(ErrorCode.TOKEN_INVALID));
    }

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotRoleException(NotRoleException e) {
        log.warn("Missing role: role={}", e.getRole());
        return R.fail(ErrorCode.PERMISSION_DENIED.getCode(), i18nUtil.getMessage(
                "error.permission.denied.role", "缺少角色: " + e.getRole(), e.getRole()));
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleNotPermissionException(NotPermissionException e) {
        log.warn("Missing permission: permission={}", e.getPermission());
        return R.fail(ErrorCode.PERMISSION_DENIED.getCode(), i18nUtil.getMessage(
                "error.permission.denied.permission", "缺少权限: " + e.getPermission(), e.getPermission()));
    }

    // ==================== 数据库异常 ====================

    @ExceptionHandler(DuplicateKeyException.class)
    public R<?> handleDuplicateKeyException(DuplicateKeyException e) {
        ErrorCode ec = resolveDuplicateErrorCode(e.getMessage());
        log.warn("Duplicate key: code={}", ec.getCode());
        return R.fail(ec.getCode(), localize(ec));
    }

    // ==================== 静态资源未找到 ====================

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNoResource(NoResourceFoundException e) {
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), i18nUtil.getMessage(
                "error.api.not.found", "接口不存在: " + e.getResourcePath(), e.getResourcePath()));
    }

    // ==================== 兜底异常 ====================

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("Unhandled exception: ", e);
        return R.fail(ErrorCode.SYSTEM_ERROR.getCode(), localize(ErrorCode.SYSTEM_ERROR));
    }

    // ==================== 私有方法 ====================

    /** 从驱动异常文本里认出唯一约束名，映射到具体 ErrorCode；认不出退 DATA_EXISTS */
    private ErrorCode resolveDuplicateErrorCode(String msg) {
        if (msg == null || msg.isEmpty()) return ErrorCode.DATA_EXISTS;
        Matcher pgMatcher = PG_PATTERN.matcher(msg);
        if (pgMatcher.find()) {
            return CONSTRAINT_MAP.getOrDefault(pgMatcher.group(1), ErrorCode.DATA_EXISTS);
        }
        Matcher mysqlMatcher = MYSQL_PATTERN.matcher(msg);
        if (mysqlMatcher.find()) {
            return CONSTRAINT_MAP.getOrDefault(mysqlMatcher.group(1), ErrorCode.DATA_EXISTS);
        }
        return ErrorCode.DATA_EXISTS;
    }
}
