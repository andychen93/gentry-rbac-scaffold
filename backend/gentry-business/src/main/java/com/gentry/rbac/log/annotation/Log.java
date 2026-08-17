package com.gentry.rbac.log.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    String module() default "";
    String type() default "OTHER";
    String title() default "";
    boolean saveResult() default false;
}
