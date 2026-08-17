package com.gentry.core.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 *
 * <p>使用示例：</p>
 * <pre>
 * // 登录接口：每 IP 每分钟 5 次
 * &#64;RateLimit(keyType = RateLimitKeyType.IP, count = 5, period = 60,
 *            message = "登录尝试过于频繁")
 * public R&lt;LoginVO&gt; login(...) { ... }
 *
 * // 全局每秒 50 次
 * &#64;RateLimit(keyType = RateLimitKeyType.GLOBAL, count = 50, period = 1)
 * public R&lt;?&gt; realtime() { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 key 自定义后缀（可选，用于区分同名方法的不同场景） */
    String key() default "";

    /** 限流维度，默认按 IP */
    RateLimitKeyType keyType() default RateLimitKeyType.IP;

    /** 时间窗口内允许的最大请求次数，默认 100 */
    int count() default 100;

    /** 时间窗口大小（秒），默认 60 */
    int period() default 60;

    /** 超限提示信息（不指定则使用全局默认） */
    String message() default "";
}
