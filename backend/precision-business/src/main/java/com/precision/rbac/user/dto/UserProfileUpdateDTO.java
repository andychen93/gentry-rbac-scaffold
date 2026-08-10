package com.precision.rbac.user.dto;

/**
 * 当前登录用户修改个人资料 DTO。
 *
 * <p>仅含个人可改字段（昵称/手机/邮箱/性别/职务）；用户名、密码、角色、部门、状态
 * 不在其中——这些由管理员在用户管理界面维护。</p>
 */
public class UserProfileUpdateDTO {

    private String nickname;
    private String phone;
    private String email;
    private Integer gender;
    private String postName;

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
}
