package com.gentry.rbac.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RoleStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值最小为0")
    @Max(value = 1, message = "状态值最大为1")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
