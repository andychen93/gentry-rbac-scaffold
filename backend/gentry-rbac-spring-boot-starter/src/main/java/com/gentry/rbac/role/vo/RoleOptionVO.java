package com.gentry.rbac.role.vo;

/**
 * 角色下拉选项 VO（轻量，仅 id/编码/名称）。
 */
public class RoleOptionVO {

    private Long id;
    private String roleCode;
    private String roleName;

    public RoleOptionVO() {}

    public RoleOptionVO(Long id, String roleCode, String roleName) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
