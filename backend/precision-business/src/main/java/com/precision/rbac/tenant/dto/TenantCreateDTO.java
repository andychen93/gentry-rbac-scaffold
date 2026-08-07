package com.precision.rbac.tenant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class TenantCreateDTO {

    @NotBlank(message = "租户编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "租户编码6-20字符，仅字母数字")
    private String code;

    @NotBlank(message = "租户名称不能为空")
    @Size(min = 2, max = 100, message = "租户名称2-100字符")
    private String name;

    @Size(max = 50, message = "联系人最长50字符")
    private String contact;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private LocalDateTime expireTime;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
