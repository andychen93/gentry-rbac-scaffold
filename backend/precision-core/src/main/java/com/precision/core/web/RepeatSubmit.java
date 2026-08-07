package com.precision.core.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 重复提交防护注解。
 *
 * <p>用于 POST/PUT/DELETE Controller 方法，基于请求指纹
 * （userId + method + path + params MD5）在 {@link #interval()} 秒内检测重复请求并拒绝。</p>
 *
 * <p>示例：</p>
 * <pre>
 * &#64;RepeatSubmit(interval = 5, message = "操作过于频繁")
 * &#64;PostMapping("/users")
 * public R&lt;?&gt; createUser(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /** 防重间隔（秒），默认 3 秒 */
    int interval() default 3;

    /** 重复提交提示信息（不指定则使用全局默认） */
    String message() default "";
}
