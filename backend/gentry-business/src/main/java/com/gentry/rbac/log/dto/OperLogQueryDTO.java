package com.gentry.rbac.log.dto;

import java.time.LocalDateTime;

public class OperLogQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String module;
    private String type;
    private String operator;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public int getOffset() { return (pageNum - 1) * pageSize; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
