package com.precision.rbac.tenant.vo;

/**
 * 租户选项 VO（登录页下拉框，最小字段）
 */
public class TenantOptionVO {

    private String code;
    private String name;

    public TenantOptionVO() {}
    public TenantOptionVO(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
