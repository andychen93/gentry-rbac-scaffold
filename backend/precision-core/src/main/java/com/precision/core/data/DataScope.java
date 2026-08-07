package com.precision.core.data;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解。
 *
 * <p>标注在 Service 方法上，AOP 切面根据当前用户角色的 {@code data_scope}
 * 自动追加 SQL 过滤条件到方法参数中的 QueryWrapper。</p>
 *
 * <p>支持的 data_scope：</p>
 * <ol>
 *     <li>全部数据（不追加条件）</li>
 *     <li>本部门及子部门（{@code dept_id IN (...)} ）</li>
 *     <li>仅本部门（{@code dept_id = ?} ）</li>
 *     <li>仅本人（{@code create_by = ?} ）</li>
 *     <li>自定义部门（当前版本按"本部门及子部门"处理，后续扩展）</li>
 * </ol>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /** 部门 ID 字段名，默认 {@code dept_id} */
    String deptIdField() default "dept_id";

    /** 创建人字段名，默认 {@code create_by}（用于"仅本人"场景） */
    String createByField() default "create_by";

    /** 表别名（多表关联时使用，如 {@code v.dept_id}） */
    String tableAlias() default "";
}
