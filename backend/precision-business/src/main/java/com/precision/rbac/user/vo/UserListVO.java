package com.precision.rbac.user.vo;

import java.time.LocalDateTime;
import java.util.List;

public class UserListVO {
    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private Integer gender;
    private String postName;
    private Long deptId;
    private String deptName;
    private Integer status;
    private LocalDateTime createTime;
    private List<RoleVO> roles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<RoleVO> getRoles() { return roles; }
    public void setRoles(List<RoleVO> roles) { this.roles = roles; }
}
