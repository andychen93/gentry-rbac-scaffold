package com.precision.rbac.user.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$", message = "用户名4-20字符，字母开头，字母数字下划线")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称2-20字符")
    private String nickname;

    private Long deptId;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer gender;

    @Size(max = 50, message = "职务最长50字符")
    private String postName;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
             message = "密码8-20位，必须含大小写字母和数字")
    private String password;

    private Integer status;
    private List<Long> roleIds;

    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
