package com.gentry.rbac.tenant.vo;

public class TenantStatistics {

    private Integer userCount;
    private Integer deptCount;
    private Integer roleCount;

    public TenantStatistics() {}

    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }
    public Integer getDeptCount() { return deptCount; }
    public void setDeptCount(Integer deptCount) { this.deptCount = deptCount; }
    public Integer getRoleCount() { return roleCount; }
    public void setRoleCount(Integer roleCount) { this.roleCount = roleCount; }
}
