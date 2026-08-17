package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.*;

public class RoleCreateDTO {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,49}$", message = "角色编码格式错误：字母开头，2-50字符，仅字母数字下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称长度为2-50字符")
    private String roleName;

    private Integer dataScope;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序最小为0")
    @Max(value = 999, message = "排序最大为999")
    private Integer sort;

    private Integer status;

    @Size(max = 500, message = "备注最长500字符")
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
