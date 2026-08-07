package com.precision.rbac.role.vo;

import java.time.LocalDateTime;

public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer dataScope;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
