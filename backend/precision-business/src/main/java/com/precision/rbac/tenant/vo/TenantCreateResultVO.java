package com.precision.rbac.tenant.vo;

public class TenantCreateResultVO {

    private Long tenantId;
    private String tenantCode;
    private String adminUsername;
    private String adminPassword;
    private String message;

    public TenantCreateResultVO() {}

    public TenantCreateResultVO(Long tenantId, String tenantCode, String adminUsername, String adminPassword) {
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.message = "租户创建成功，请妥善保管管理员密码";
    }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
