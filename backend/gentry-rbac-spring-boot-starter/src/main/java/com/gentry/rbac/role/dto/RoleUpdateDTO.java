package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.*;

public class RoleUpdateDTO {

    @NotBlank(message = "{valid.role.roleName.notBlank}")
    @Size(min = 2, max = 50, message = "{valid.role.roleName.size}")
    private String roleName;

    @NotNull(message = "{valid.common.sort.notNull}")
    @Min(value = 0, message = "{valid.role.sort.min}")
    @Max(value = 999, message = "{valid.role.sort.max}")
    private Integer sort;

    @Size(max = 500, message = "{valid.common.remark.size}")
    private String remark;

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
