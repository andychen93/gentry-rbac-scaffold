package com.gentry.rbac.tenant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class TenantCreateDTO {

    @NotBlank(message = "{valid.tenant.code.notBlank}")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "{valid.tenant.code.pattern}")
    private String code;

    @NotBlank(message = "{valid.tenant.name.notBlank}")
    @Size(min = 2, max = 100, message = "{valid.tenant.name.size}")
    private String name;

    @Size(max = 50, message = "{valid.tenant.contact.size}")
    private String contact;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{valid.common.phone.pattern}")
    private String phone;

    @Email(message = "{valid.common.email.email}")
    private String email;

    private LocalDateTime expireTime;

    @Size(max = 500, message = "{valid.common.remark.size}")
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
