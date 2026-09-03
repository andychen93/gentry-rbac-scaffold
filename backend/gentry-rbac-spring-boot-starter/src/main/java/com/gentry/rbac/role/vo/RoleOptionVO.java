package com.gentry.rbac.role.vo;

import com.gentry.core.i18n.RoleI18nKeyResolver;

/**
 * 角色下拉选项 VO（轻量，仅 id/编码/名称）。
 */
public class RoleOptionVO {

    private Long id;
    private String roleCode;
    private String roleName;
    /** 见 {@link RoleListVO#getI18nKey()} 的注释 */
    private String i18nKey;

    public RoleOptionVO() {}

    public RoleOptionVO(Long id, String roleCode, String roleName) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.i18nKey = RoleI18nKeyResolver.resolve(roleCode);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getI18nKey() { return i18nKey; }
    public void setI18nKey(String i18nKey) { this.i18nKey = i18nKey; }
}
