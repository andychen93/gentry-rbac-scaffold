package com.gentry.core.tenant;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略租户条件注解
 * <p>
 * 第 2 层租户跳过机制：标注在方法或类上，执行时不追加 tenant_id 条件。
 * 通过 IgnoreTenantAspect 在 AOP 切面中调用 TenantManager.ignoreTenantCondition()。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenant {

    /**
     * 备注说明（可选）
     */
    String value() default "";
}
