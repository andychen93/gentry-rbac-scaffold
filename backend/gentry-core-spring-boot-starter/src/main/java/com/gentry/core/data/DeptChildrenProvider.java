package com.gentry.core.data;

import java.util.List;

/**
 * 部门树下级查询 SPI。
 *
 * <p>gentry-core 不感知具体的部门表实现，由 gentry-rbac-spring-boot-starter 的
 * {@code DeptService} 注册一个实现 Bean 给 DataScopeAspect 使用。</p>
 */
public interface DeptChildrenProvider {

    /**
     * 返回指定部门的自身 + 所有下级部门 ID。
     *
     * @param deptId 部门 ID
     * @return 包含自身及子部门的 ID 列表，不存在返回空列表
     */
    List<Long> getSelfAndChildrenIds(Long deptId);
}
