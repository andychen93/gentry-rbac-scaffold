package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class RoleMenuAssignDTO {

    @NotNull(message = "{valid.role.menuIds.notNull}")
    private List<Long> menuIds;

    public List<Long> getMenuIds() { return menuIds; }
    public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }
}
