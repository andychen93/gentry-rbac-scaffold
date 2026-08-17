package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.*;

public class RoleCreateDTO {

    @NotBlank(message = "{valid.role.roleCode.notBlank}")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,49}$", message = "{valid.role.roleCode.pattern}")
    private String roleCode;

    @NotBlank(message = "{valid.role.roleName.notBlank}")
    @Size(min = 2, max = 50, message = "{valid.role.roleName.size}")
    private String roleName;

    private Integer dataScope;

    @NotNull(message = "{valid.common.sort.notNull}")
    @Min(value = 0, message = "{valid.role.sort.min}")
    @Max(value = 999, message = "{valid.role.sort.max}")
    private Integer sort;

    private Integer status;

    @Size(max = 500, message = "{valid.common.remark.size}")
    private String remark;

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
}
