package com.gentry.core.data;

import com.gentry.core.security.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("数据权限切面")
class DataScopeAspectTest {

    private UserDataScopeResolver scopeResolver;
    private DeptChildrenProvider deptProvider;
    private DataScopeAspect aspect;

    @BeforeEach
    void setup() {
        scopeResolver = mock(UserDataScopeResolver.class);
        deptProvider = mock(DeptChildrenProvider.class);

        ObjectProvider<UserDataScopeResolver> scopeOp = singleProvider(scopeResolver);
        ObjectProvider<DeptChildrenProvider> deptOp = singleProvider(deptProvider);
        aspect = new DataScopeAspect(scopeOp, deptOp);

        UserContext.setUserId(100L);
        UserContext.setTenantId(1L);
        UserContext.setDeptId(50L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        DataScopeContext.clear();
    }

    @Test
    @DisplayName("scope=ALL 设置全部数据条件")
    void allData() throws Throwable {
        when(scopeResolver.resolveDataScope(100L, 1L)).thenReturn(DataScopeAspect.SCOPE_ALL);
        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {
            DataScopeContext.Condition c = DataScopeContext.get();
            assertThat(c).isNotNull();
            assertThat(c.allData()).isTrue();
        });

        aspect.around(pjp, dataScope(""));
    }

    @Test
    @DisplayName("scope=DEPT_AND_CHILD 下发部门+子部门 ID 列表")
    void deptAndChildren() throws Throwable {
        when(scopeResolver.resolveDataScope(100L, 1L)).thenReturn(DataScopeAspect.SCOPE_DEPT_AND_CHILD);
        when(deptProvider.getSelfAndChildrenIds(1L, 50L)).thenReturn(List.of(50L, 51L, 52L));

        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {
            DataScopeContext.Condition c = DataScopeContext.get();
            assertThat(c.hasDeptFilter()).isTrue();
            assertThat(c.deptIds()).containsExactly(50L, 51L, 52L);
            assertThat(c.deptIdField()).isEqualTo("dept_id");
        });
        aspect.around(pjp, dataScope(""));
    }

    @Test
    @DisplayName("scope=DEPT 仅本部门")
    void deptOnly() throws Throwable {
        when(scopeResolver.resolveDataScope(100L, 1L)).thenReturn(DataScopeAspect.SCOPE_DEPT);

        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {
            DataScopeContext.Condition c = DataScopeContext.get();
            assertThat(c.deptIds()).containsExactly(50L);
        });
        aspect.around(pjp, dataScope(""));
    }

    @Test
    @DisplayName("scope=SELF 仅本人（按 create_by 过滤）")
    void selfOnly() throws Throwable {
        when(scopeResolver.resolveDataScope(100L, 1L)).thenReturn(DataScopeAspect.SCOPE_SELF);

        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {
            DataScopeContext.Condition c = DataScopeContext.get();
            assertThat(c.hasCreatorFilter()).isTrue();
            assertThat(c.creatorUserId()).isEqualTo(100L);
            assertThat(c.createByField()).isEqualTo("create_by");
        });
        aspect.around(pjp, dataScope(""));
    }

    @Test
    @DisplayName("tableAlias 会加到字段名前")
    void tableAliasPrepended() throws Throwable {
        when(scopeResolver.resolveDataScope(100L, 1L)).thenReturn(DataScopeAspect.SCOPE_DEPT);

        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {
            DataScopeContext.Condition c = DataScopeContext.get();
            assertThat(c.deptIdField()).isEqualTo("v.dept_id");
        });
        aspect.around(pjp, dataScope("v"));
    }

    @Test
    @DisplayName("未登录跳过，DataScopeContext 为空")
    void notLoggedIn_skip() throws Throwable {
        UserContext.clear();
        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {
            assertThat(DataScopeContext.get()).isNull();
        });
        aspect.around(pjp, dataScope(""));
    }

    @Test
    @DisplayName("方法执行完自动清理上下文")
    void cleanup_afterInvocation() throws Throwable {
        when(scopeResolver.resolveDataScope(100L, 1L)).thenReturn(DataScopeAspect.SCOPE_ALL);
        ProceedingJoinPoint pjp = pjpReturning("ok", () -> {});
        aspect.around(pjp, dataScope(""));
        assertThat(DataScopeContext.get()).isNull();
    }

    // ============ helpers ============

    private DataScope dataScope(String alias) {
        return new DataScope() {
            @Override public Class<? extends Annotation> annotationType() { return DataScope.class; }
            @Override public String deptIdField() { return "dept_id"; }
            @Override public String createByField() { return "create_by"; }
            @Override public String tableAlias() { return alias; }
        };
    }

    private ProceedingJoinPoint pjpReturning(Object result, Runnable verifyInside) throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenAnswer(inv -> {
            verifyInside.run();
            return result;
        });
        return pjp;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> singleProvider(T instance) {
        ObjectProvider<T> op = mock(ObjectProvider.class);
        when(op.getIfAvailable()).thenReturn(instance);
        return op;
    }
}
