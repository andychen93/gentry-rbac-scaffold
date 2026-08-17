package com.gentry.rbac.tenant.dto;

import jakarta.validation.constraints.NotNull;

public class TenantStatusDTO {

    @NotNull(message = "{valid.common.status.notNull}")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
