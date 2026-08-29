package com.gentry.rbac.tenant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class TenantQueryDTO {

    @Min(value = 1, message = "{valid.common.pageNum.min}")
    private Integer pageNum = 1;

    @Min(value = 1, message = "{valid.common.pageSize.min}")
    @Max(value = 100, message = "{valid.common.pageSize.max}")
    private Integer pageSize = 10;

    private String name;
    private String code;
    private String contact;
    private String phone;
    private Integer status;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public int getOffset() { return (pageNum - 1) * pageSize; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
