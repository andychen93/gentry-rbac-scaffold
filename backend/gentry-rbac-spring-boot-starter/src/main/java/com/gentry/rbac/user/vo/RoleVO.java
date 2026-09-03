package com.gentry.rbac.user.vo;

import com.gentry.core.i18n.RoleI18nKeyResolver;

public class RoleVO {
    private Long id;
    private String roleName;
    private String roleCode;
    /** 见 {@code com.gentry.rbac.role.vo.RoleListVO#i18nKey} 的注释 */
    private String i18nKey;

    public RoleVO() {}
    public RoleVO(Long id, String roleName, String roleCode) {
        this.id = id;
        this.roleName = roleName;
        this.roleCode = roleCode;
        this.i18nKey = RoleI18nKeyResolver.resolve(roleCode);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getI18nKey() { return i18nKey; }
    public void setI18nKey(String i18nKey) { this.i18nKey = i18nKey; }
}
