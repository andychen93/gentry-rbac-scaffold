package com.precision.rbac.log.vo;

import java.time.LocalDateTime;

public class LoginLogDetailVO {
    private Long id; private String username; private String loginType; private String loginIp;
    private String location; private String browser; private String os; private String deviceType;
    private String userAgent; private Integer status; private String message; private LocalDateTime loginTime;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
    public String getLoginType() { return loginType; } public void setLoginType(String loginType) { this.loginType = loginType; }
    public String getLoginIp() { return loginIp; } public void setLoginIp(String loginIp) { this.loginIp = loginIp; }
    public String getLocation() { return location; } public void setLocation(String location) { this.location = location; }
    public String getBrowser() { return browser; } public void setBrowser(String browser) { this.browser = browser; }
    public String getOs() { return os; } public void setOs(String os) { this.os = os; }
    public String getDeviceType() { return deviceType; } public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getUserAgent() { return userAgent; } public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
    public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    public LocalDateTime getLoginTime() { return loginTime; } public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}
