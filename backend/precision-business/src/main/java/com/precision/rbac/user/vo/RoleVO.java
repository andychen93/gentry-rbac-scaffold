package com.precision.rbac.user.vo;

public class RoleVO {
    private Long id;
    private String roleName;
    private String roleCode;

    public RoleVO() {}
    public RoleVO(Long id, String roleName, String roleCode) {
        this.id = id;
        this.roleName = roleName;
        this.roleCode = roleCode;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
}
