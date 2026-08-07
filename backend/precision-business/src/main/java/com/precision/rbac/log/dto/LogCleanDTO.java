package com.precision.rbac.log.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LogCleanDTO {
    @NotNull(message = "清理天数不能为空")
    @Min(value = 30, message = "至少保留30天日志")
    private Integer beforeDays;

    public Integer getBeforeDays() { return beforeDays; }
    public void setBeforeDays(Integer beforeDays) { this.beforeDays = beforeDays; }
}
