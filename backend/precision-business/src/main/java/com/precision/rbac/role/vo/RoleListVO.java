package com.precision.rbac.role.vo;

import java.time.LocalDateTime;

public class RoleListVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer dataScope;
    private String dataScopeName;
    private Integer userCount;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public String getDataScopeName() { return dataScopeName; }
    public void setDataScopeName(String dataScopeName) { this.dataScopeName = dataScopeName; }
    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
