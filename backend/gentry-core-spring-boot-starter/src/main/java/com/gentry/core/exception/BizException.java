package com.gentry.core.exception;

import com.gentry.core.common.ErrorCode;

/**
 * 业务异常。
 *
 * <p>消息有两个来源，都会被 {@code GlobalExceptionHandler} 本地化：</p>
 * <ol>
 *   <li>不传 {@code messageKey}：用 {@link ErrorCode#i18nKey()}，由枚举名机械派生</li>
 *   <li>传 {@code messageKey}：用它。适用于复用同一个 {@link ErrorCode}（多为
 *       {@code PARAM_ERROR}）但需要区分具体提示的场景</li>
 * </ol>
 *
 * <pre>
 * throw new BizException(ErrorCode.ROLE_NOT_FOUND);
 * throw new BizException(ErrorCode.PARAM_ERROR, "error.role.builtin.undeletable");
 * throw new BizException(ErrorCode.ROLE_NOT_FOUND, "error.role.not.found.detail", missing);
 * </pre>
 *
 * <p><b>第二个参数是 key 不是文案。</b>本类历史上曾接受裸中文字符串，那样的消息无法翻译；
 * 现已全部改为 key。key 必须以 {@code error.} 开头且在 {@code error_*.properties} 中存在，
 * 由 {@code BizExceptionMessageKeyTest} 扫描保证 —— 拼错 key 不会编译报错，
 * 只会在切换语言时才暴露。</p>
 */
public class BizException extends RuntimeException {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private final ErrorCode errorCode;
    private final int code;
    /** 显式指定的 i18n key；为 null 时用 {@code errorCode.i18nKey()} */
    private final String messageKey;
    /** MessageFormat 参数，按 {0} {1} 顺序对应 */
    private final Object[] args;

    public BizException(ErrorCode errorCode) {
        this(errorCode, null, EMPTY_ARGS);
    }

    /**
     * 用 {@link ErrorCode} 派生的 key + 参数。
     *
     * <p>写成静态工厂而不是构造器 {@code BizException(ErrorCode, Object...)}：后者与
     * {@code BizException(ErrorCode, String)} 在传单个字符串时会重载歧义。</p>
     *
     * <pre>
     * throw BizException.of(ErrorCode.ACCOUNT_LOCKED, lockMinutes);
     * // error.account.locked=账号已被锁定，请 {0} 分钟后再试
     * </pre>
     */
    public static BizException of(ErrorCode errorCode, Object... args) {
        return new BizException(errorCode, null, args);
    }

    public BizException(ErrorCode errorCode, String messageKey) {
        this(errorCode, messageKey, EMPTY_ARGS);
    }

    public BizException(ErrorCode errorCode, String messageKey, Object... args) {
        // super message 存 key（或兜底中文），便于日志溯源；对外响应由 handler 本地化后给出
        super(messageKey != null ? messageKey : errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
        this.messageKey = messageKey;
        this.args = (args == null) ? EMPTY_ARGS : args;
    }

    public ErrorCode getErrorCode() { return errorCode; }

    public int getCode() { return code; }

    /** 实际生效的 i18n key：显式指定优先，否则由 ErrorCode 派生 */
    public String resolveI18nKey() {
        return messageKey != null ? messageKey : errorCode.i18nKey();
    }

    /** 显式指定的 key，未指定返回 null */
    public String getMessageKey() { return messageKey; }

    public Object[] getArgs() { return args; }

    /** 语言包整体缺失时的兜底文案 */
    public String getFallbackMessage() { return errorCode.getMessage(); }
}
