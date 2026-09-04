package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class RoleQueryDTO {

    @Min(value = 1, message = "{valid.common.pageNum.min}")
    private Integer pageNum = 1;

    @Min(value = 1, message = "{valid.common.pageSize.min}")
    @Max(value = 100, message = "{valid.common.pageSize.max}")
    private Integer pageSize = 10;

    private String roleName;
    private String roleCode;
    private Integer status;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
