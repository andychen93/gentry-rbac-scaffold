package com.precision.rbac.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UserRoleAssignDTO {

    @NotNull(message = "角色ID列表不能为null")
    private List<Long> roleIds;

    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
