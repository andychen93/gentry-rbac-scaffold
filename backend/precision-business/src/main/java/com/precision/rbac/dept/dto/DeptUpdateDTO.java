package com.precision.rbac.dept.dto;

import jakarta.validation.constraints.*;

/**
 * 编辑部门 DTO
 */
public class DeptUpdateDTO {

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(min = 2, max = 50, message = "部门名称长度为2-50字符")
    private String name;

    private Long leaderId;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号格式")
    private String phone;

    @Email(message = "请输入正确的邮箱格式")
    private String email;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序值最小为0")
    @Max(value = 999, message = "排序值最大为999")
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
