package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RoleDataScopeDTO {

    @NotNull(message = "{valid.role.dataScope.notNull}")
    @Min(value = 1, message = "{valid.role.dataScope.min}")
    @Max(value = 5, message = "{valid.role.dataScope.max}")
    private Integer dataScope;

    private List<Long> deptIds;

    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
}
