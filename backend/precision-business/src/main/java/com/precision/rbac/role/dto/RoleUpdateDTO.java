package com.precision.rbac.role.dto;

import jakarta.validation.constraints.*;

public class RoleUpdateDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 2, max = 50, message = "角色名称长度为2-50字符")
    private String roleName;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序最小为0")
    @Max(value = 999, message = "排序最大为999")
    private Integer sort;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
