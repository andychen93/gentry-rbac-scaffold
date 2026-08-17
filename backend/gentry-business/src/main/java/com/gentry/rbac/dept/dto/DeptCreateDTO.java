package com.gentry.rbac.dept.dto;

import jakarta.validation.constraints.*;

/**
 * 新增部门 DTO
 */
public class DeptCreateDTO {

    private Long parentId;

    @NotBlank(message = "{valid.dept.name.notBlank}")
    @Size(min = 2, max = 50, message = "{valid.dept.name.size}")
    private String name;

    private Long leaderId;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{valid.dept.phone.pattern}")
    private String phone;

    @Email(message = "{valid.dept.email.email}")
    private String email;

    @NotNull(message = "{valid.common.sort.notNull}")
    @Min(value = 0, message = "{valid.common.sort.min}")
    @Max(value = 999, message = "{valid.common.sort.max}")
    private Integer sort;

    private Integer status;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
