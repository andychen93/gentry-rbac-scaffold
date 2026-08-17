package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色绑定用户入参（全量覆盖：先删该角色的全部关联，再插入本次列表）。
 */
public class RoleUserAssignDTO {

    /** 允许传空集合表示「解绑全部用户」，但不允许为 null */
    @NotNull(message = "用户ID列表不能为null")
    private List<Long> userIds;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
}
