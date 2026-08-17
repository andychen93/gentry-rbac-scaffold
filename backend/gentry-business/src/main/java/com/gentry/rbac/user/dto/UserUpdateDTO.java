package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class UserUpdateDTO {

    @NotBlank(message = "{valid.user.nickname.notBlank}")
    @Size(min = 2, max = 20, message = "{valid.user.nickname.size}")
    private String nickname;

    private Long deptId;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "{valid.common.phone.pattern}")
    private String phone;

    @Email(message = "{valid.common.email.email}")
    private String email;

    private Integer gender;

    @Size(max = 50, message = "{valid.user.postName.size}")
    private String postName;

    private Integer status;
    private List<Long> roleIds;

    @Size(max = 500, message = "{valid.common.remark.size}")
    private String remark;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
