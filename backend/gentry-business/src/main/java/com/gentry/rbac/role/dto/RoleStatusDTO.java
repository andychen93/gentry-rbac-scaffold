package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RoleStatusDTO {

    @NotNull(message = "{valid.common.status.notNull}")
    @Min(value = 0, message = "{valid.role.status.min}")
    @Max(value = 1, message = "{valid.role.status.max}")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
