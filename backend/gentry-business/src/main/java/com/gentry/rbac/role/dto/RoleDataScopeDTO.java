package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RoleDataScopeDTO {

    @NotNull(message = "数据权限范围不能为空")
    @Min(value = 1, message = "数据权限范围最小为1")
    @Max(value = 5, message = "数据权限范围最大为5")
    private Integer dataScope;

    private List<Long> deptIds;

    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public List<Long> getDeptIds() { return deptIds; }
    public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
}
