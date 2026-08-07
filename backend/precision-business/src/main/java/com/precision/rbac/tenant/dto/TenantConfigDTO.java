package com.precision.rbac.tenant.dto;

import jakarta.validation.constraints.Min;

public class TenantConfigDTO {

    @Min(value = 1, message = "最大设备数最小为1")
    private Integer maxDevices;

    @Min(value = 1, message = "最大用户数最小为1")
    private Integer maxUsers;

    @Min(value = 1, message = "数据保留天数最小为1")
    private Integer dataRetentionDays;

    private Boolean videoEnabled;
    private Boolean alarmEnabled;
    private Boolean reportEnabled;
    private String mapProvider;

    public Integer getMaxDevices() { return maxDevices; }
    public void setMaxDevices(Integer maxDevices) { this.maxDevices = maxDevices; }
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    public Integer getDataRetentionDays() { return dataRetentionDays; }
    public void setDataRetentionDays(Integer dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }
    public Boolean getVideoEnabled() { return videoEnabled; }
    public void setVideoEnabled(Boolean videoEnabled) { this.videoEnabled = videoEnabled; }
    public Boolean getAlarmEnabled() { return alarmEnabled; }
    public void setAlarmEnabled(Boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }
    public Boolean getReportEnabled() { return reportEnabled; }
    public void setReportEnabled(Boolean reportEnabled) { this.reportEnabled = reportEnabled; }
    public String getMapProvider() { return mapProvider; }
    public void setMapProvider(String mapProvider) { this.mapProvider = mapProvider; }
}
