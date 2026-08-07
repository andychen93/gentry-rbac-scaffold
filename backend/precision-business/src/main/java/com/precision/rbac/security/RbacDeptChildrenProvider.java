package com.precision.rbac.security;

import com.precision.core.data.DeptChildrenProvider;
import com.precision.rbac.dept.service.DeptService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 {@link DeptService#getChildDeptIds} 的 {@link DeptChildrenProvider} 实现，
 * 供 DataScope 切面使用。
 */
@Component
public class RbacDeptChildrenProvider implements DeptChildrenProvider {

    private final DeptService deptService;

    public RbacDeptChildrenProvider(DeptService deptService) {
        this.deptService = deptService;
    }

    @Override
    public List<Long> getSelfAndChildrenIds(Long tenantId, Long deptId) {
        if (deptId == null) return List.of();
        // DeptServiceImpl.getChildDeptIds 内部已经包含 deptId 自身
        return deptService.getChildDeptIds(deptId);
    }
}
