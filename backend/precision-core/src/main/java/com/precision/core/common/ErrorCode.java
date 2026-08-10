package com.precision.core.common;

/**
 * 错误码枚举
 *
 * 编号规范（见 CLAUDE.md 附录 A）：
 * 0           = SUCCESS
 * 10001-10099 = 系统错误
 * 20001-20099 = RBAC 业务错误
 * 30001-30099 = 认证/数据错误
 * 40001-40099 = 安全控制
 * 50001-50099 = 设备错误（预留）
 */
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(0, "操作成功"),

    // ==================== 系统错误 10001-10099 ====================
    SYSTEM_ERROR(10001, "系统异常"),
    PARAM_ERROR(10002, "参数校验失败"),
    METHOD_NOT_ALLOWED(10003, "请求方法不允许"),
    PARAM_TYPE_MISMATCH(10004, "请求参数类型不匹配"),
    HTTP_MESSAGE_NOT_READABLE(10005, "HTTP消息不可读"),

    // ==================== RBAC 业务错误 20001-20099 ====================
    TENANT_NOT_FOUND(20001, "租户不存在"),
    TENANT_CODE_EXISTS(20002, "租户编码已存在"),
    TENANT_DISABLED(20003, "租户已禁用"),
    TENANT_EXPIRED(20004, "租户已过期"),
    ROLE_CODE_EXISTS(20005, "角色编码已存在"),
    ROLE_IN_USE(20006, "角色正在使用中"),
    USERNAME_EXISTS(20007, "用户名已存在"),
    PHONE_EXISTS(20008, "手机号已存在"),
    USER_DISABLED(20009, "用户已禁用"),
    OLD_PASSWORD_ERROR(20010, "原密码错误"),
    DEPT_HAS_CHILDREN(20011, "部门存在子部门"),
    DEPT_NAME_EXISTS(20012, "部门名称已存在"),
    USER_NOT_FOUND(20013, "用户不存在"),
    MENU_HAS_CHILDREN(20014, "菜单存在子菜单"),
    ROLE_NOT_FOUND(20015, "角色不存在"),
    DICT_TYPE_EXISTS(20016, "字典类型已存在"),
    DICT_TYPE_IN_USE(20017, "字典类型正在使用中"),
    LOGIN_FAILED(20018, "用户名或密码错误"),
    ACCOUNT_LOCKED(20019, "账号已被锁定，请稍后再试"),
    CAPTCHA_ERROR(20020, "验证码错误或已过期"),
    PASSWORD_EXPIRED(20021, "密码已过期，请修改密码"),

    // ==================== 认证/数据错误 30001-30099 ====================
    TOKEN_INVALID(30001, "Token无效"),
    TOKEN_EXPIRED(30002, "Token已过期"),
    PERMISSION_DENIED(30003, "权限不足"),
    DATA_EXISTS(30010, "数据已存在"),
    DATA_NOT_FOUND(30011, "数据不存在"),

    // ==================== 安全控制 40001-40099 ====================
    TOO_MANY_REQUESTS(40001, "请求过于频繁"),
    DUPLICATE_SUBMIT(40002, "重复提交"),
    CANNOT_DELETE_SELF(40003, "不能删除当前登录用户"),

    // ==================== 设备错误 50001-50099 ====================
    DEVICE_NOT_FOUND(50001, "设备不存在"),
    DEVICE_SN_EXISTS(50002, "设备SN已存在"),
    DEVICE_SIM_EXISTS(50003, "SIM卡号已存在"),
    DEVICE_ALREADY_BOUND(50004, "设备已绑定"),
    DEVICE_NOT_BOUND(50005, "设备未绑定"),
    VEHICLE_ALREADY_BOUND(50006, "车辆已绑定其他设备"),
    DEVICE_DISABLED(50007, "设备已停用"),

    // ==================== 厂商/鉴权错误 50020-50049 ====================
    VENDOR_NOT_FOUND(50020, "厂商不存在"),
    VENDOR_CODE_EXISTS(50021, "厂商CODE已存在"),
    VENDOR_DISABLED(50022, "厂商已禁用"),
    VENDOR_STRATEGY_NOT_FOUND(50023, "厂商对应的解析策略不存在"),
    VENDOR_CODE_MISMATCH(50024, "厂商CODE与解析策略不一致"),
    VENDOR_PROTOCOL_MISMATCH(50025, "厂商协议类型与解析策略不匹配"),
    VENDOR_IN_USE(50026, "厂商下存在设备，无法删除"),
    DEVICE_AUTH_EXISTS(50030, "设备已注册鉴权码"),
    DEVICE_AUTH_NOT_FOUND(50031, "设备鉴权信息不存在"),

    // ==================== 车辆错误 51001-51099 ====================
    VEHICLE_NOT_FOUND(51001, "车辆不存在"),
    VEHICLE_PLATE_EXISTS(51002, "车牌号已存在"),
    VEHICLE_HAS_DEVICE(51003, "车辆已绑定设备，请先解绑"),
    FLEET_NOT_FOUND(51004, "车队不存在"),
    FLEET_NAME_EXISTS(51005, "同级车队名称已存在"),
    FLEET_HAS_CHILDREN(51006, "车队存在子车队，无法删除"),
    FLEET_HAS_VEHICLES(51007, "车队下存在车辆，无法删除"),

    // ==================== 司机错误 51101-51199 ====================
    DRIVER_NOT_FOUND(51101, "司机不存在"),
    DRIVER_PHONE_EXISTS(51102, "司机手机号已存在"),

    // ==================== 报警错误 52001-52099 ====================
    ALARM_NOT_FOUND(52001, "报警记录不存在"),
    ALARM_ALREADY_HANDLED(52002, "报警已处理"),

    // ==================== 通知 52050-52069 ====================
    NOTIFICATION_NOT_FOUND(52050, "通知不存在"),

    // ==================== 位置错误 53001-53099 ====================
    LOCATION_TIME_RANGE_EXCEEDED(53001, "轨迹查询时间范围不能超过7天"),
    LOCATION_TRACK_EMPTY(53002, "未查询到轨迹数据"),
    LOCATION_NO_DATA(53003, "设备暂无位置数据"),

    // ==================== 轨迹导出 53010-53019 ====================
    EXPORT_RANGE_EXCEEDED(53010, "导出时间范围超过上限"),
    EXPORT_NOT_READY(53011, "导出任务尚未完成"),
    EXPORT_EXPIRED(53012, "导出文件已过期"),
    EXPORT_FILE_MISSING(53013, "导出文件不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
