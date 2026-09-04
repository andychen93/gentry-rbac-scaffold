package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UserRoleAssignDTO {

    @NotNull(message = "{valid.user.roleIds.notNull}")
    private List<Long> roleIds;

    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
