package com.precision.core.data;

import com.precision.core.security.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 数据权限切面。
 *
 * <p>在方法执行前解析当前用户的 data_scope 并写入 {@link DataScopeContext}，
 * 方法执行完（含异常）清理上下文。</p>
 *
 * <p>Mapper XML / Service 通过 {@link DataScopeContext#get()} 读取条件并拼接到 SQL。</p>
 */
@Aspect
@Component
public class DataScopeAspect {

    private static final Logger log = LoggerFactory.getLogger(DataScopeAspect.class);

    /** 数据权限语义常量 */
    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_DEPT_AND_CHILD = 2;
    public static final int SCOPE_DEPT = 3;
    public static final int SCOPE_SELF = 4;
    public static final int SCOPE_CUSTOM = 5;

    private final ObjectProvider<UserDataScopeResolver> scopeResolverProvider;
    private final ObjectProvider<DeptChildrenProvider> deptProviderProvider;

    public DataScopeAspect(ObjectProvider<UserDataScopeResolver> scopeResolverProvider,
                           ObjectProvider<DeptChildrenProvider> deptProviderProvider) {
        this.scopeResolverProvider = scopeResolverProvider;
        this.deptProviderProvider = deptProviderProvider;
    }

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        try {
            DataScopeContext.Condition condition = buildCondition(dataScope);
            if (condition != null) {
                DataScopeContext.set(condition);
            }
            return pjp.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    DataScopeContext.Condition buildCondition(DataScope annotation) {
        Long userId = UserContext.getUserId();
        Long tenantId = UserContext.getTenantId();
        Long deptId = UserContext.getDeptId();

        if (userId == null) {
            return null; // 未登录场景（如 /auth/login 内部调用）跳过
        }
        UserDataScopeResolver scopeResolver = scopeResolverProvider.getIfAvailable();
        if (scopeResolver == null) {
            log.debug("UserDataScopeResolver 未配置，DataScope 切面跳过");
            return null;
        }
        Integer scope = scopeResolver.resolveDataScope(userId, tenantId);
        if (scope == null) {
            return null;
        }

        String deptField = annotation.tableAlias().isEmpty()
                ? annotation.deptIdField()
                : annotation.tableAlias() + "." + annotation.deptIdField();
        String createByField = annotation.tableAlias().isEmpty()
                ? annotation.createByField()
                : annotation.tableAlias() + "." + annotation.createByField();

        return switch (scope) {
            case SCOPE_ALL -> DataScopeContext.Condition.all();
            case SCOPE_DEPT_AND_CHILD, SCOPE_CUSTOM -> {
                List<Long> ids = getSelfAndChildren(tenantId, deptId);
                yield DataScopeContext.Condition.byDepts(ids, deptField);
            }
            case SCOPE_DEPT -> {
                List<Long> ids = deptId == null ? List.of() : List.of(deptId);
                yield DataScopeContext.Condition.byDepts(ids, deptField);
            }
            case SCOPE_SELF -> DataScopeContext.Condition.byCreator(userId, createByField);
            default -> null;
        };
    }

    private List<Long> getSelfAndChildren(Long tenantId, Long deptId) {
        if (deptId == null) return Collections.emptyList();
        DeptChildrenProvider provider = deptProviderProvider.getIfAvailable();
        if (provider == null) return List.of(deptId); // 无 provider 时退化为仅本部门
        return provider.getSelfAndChildrenIds(tenantId, deptId);
    }
}
