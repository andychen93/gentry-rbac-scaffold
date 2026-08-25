package com.gentry.core.common;

/**
 * 错误码枚举
 *
 * 编号规范（见 CLAUDE.md 附录 A）：
 * 0           = SUCCESS
 * 10001-10099 = 系统错误
 * 20001-20099 = RBAC 与脚手架自带能力（含消息通知）
 * 30001-30099 = 认证/数据错误
 * 40001-40099 = 安全控制
 * 50001+      = 派生业务自行分配（脚手架不占用）
 *
 * <p>本枚举只允许出现权限脚手架自身的错误码。此前混入的设备/厂商/车辆/司机/
 * 报警/位置/轨迹导出等码段（50001~53013，共 27 个）是从高精度定位平台整体
 * 拷贝时带进来的，全部零引用，已清除 —— RBAC 脚手架里不该出现业务领域概念。</p>
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

    // ==================== 消息通知 20030-20039 ====================
    NOTIFICATION_NOT_FOUND(20030, "通知不存在"),

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
    /**
     * 非平台超管试图把平台级权限点（{@code sys_menu.is_platform = 1}）分配给角色。
     *
     * <p>不能只靠「新建租户时不给」—— 租户管理员握有 {@code system:role:assignMenu}，
     * 能自己在角色管理里把租户管理权限勾回来。这个码是那条自提权路径的守卫。</p>
     */
    PLATFORM_MENU_FORBIDDEN(40004, "无权分配平台级权限");

    /**
     * i18n key，由枚举名机械派生：{@code ROLE_NOT_FOUND} → {@code error.role.not.found}。
     *
     * <p>不用在每个枚举项手写 key —— 枚举常量名本身就是现成的语义 key，
     * IDE 重命名即同步改 key。译文在 {@code i18n/error_{lang}.properties}。</p>
     *
     * <p>{@link #getMessage()} 的语义随之变为<b>兜底值</b>：语言包缺失时原样返回中文，
     * 界面不会露出裸 key。完整兜底链：目标语言 → zh_CN → 本字段。</p>
     */
    public String i18nKey() {
        return "error." + name().toLowerCase(java.util.Locale.ROOT).replace('_', '.');
    }

    // 50001 起留给基于本脚手架派生的业务自行分配，脚手架自身不占用。

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
