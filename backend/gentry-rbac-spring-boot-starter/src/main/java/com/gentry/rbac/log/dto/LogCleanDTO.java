package com.gentry.rbac.log.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LogCleanDTO {
    @NotNull(message = "{valid.log.beforeDays.notNull}")
    @Min(value = 30, message = "{valid.log.beforeDays.min}")
    private Integer beforeDays;

    public Integer getBeforeDays() { return beforeDays; }
    public void setBeforeDays(Integer beforeDays) { this.beforeDays = beforeDays; }
}
