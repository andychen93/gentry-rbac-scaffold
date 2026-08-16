package com.precision.rbac.user.vo;

/**
 * 用户下拉选项 VO（轻量，仅 id/账号/昵称/部门名）。
 *
 * <p>用于「角色管理 → 绑定用户」这类穿梭框场景，与
 * {@link com.precision.rbac.role.vo.RoleOptionVO} 对称。</p>
 */
public class UserOptionVO {

    private Long id;
    private String username;
    private String nickname;
    private String deptName;

    public UserOptionVO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
}
