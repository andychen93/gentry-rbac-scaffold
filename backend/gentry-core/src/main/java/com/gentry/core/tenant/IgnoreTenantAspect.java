package com.gentry.core.tenant;

import com.mybatisflex.core.tenant.TenantManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 忽略租户条件切面
 * <p>
 * 第 2 层租户跳过机制：拦截 @IgnoreTenant 注解的方法，
 * 在方法执行期间通过 TenantManager 静态方法忽略租户条件。
 */
@Aspect
@Component
public class IgnoreTenantAspect {

    @Around("@annotation(ignoreTenant)")
    public Object aroundIgnoreTenant(ProceedingJoinPoint joinPoint, IgnoreTenant ignoreTenant) throws Throwable {
        try {
            TenantManager.ignoreTenantCondition();
            return joinPoint.proceed();
        } finally {
            TenantManager.restoreTenantCondition();
        }
    }

    @Around("@within(ignoreTenant)")
    public Object aroundIgnoreTenantClass(ProceedingJoinPoint joinPoint, IgnoreTenant ignoreTenant) throws Throwable {
        try {
            TenantManager.ignoreTenantCondition();
            return joinPoint.proceed();
        } finally {
            TenantManager.restoreTenantCondition();
        }
    }
}
