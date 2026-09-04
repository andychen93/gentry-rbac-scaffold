package com.gentry.rbac.role.vo;

import java.time.LocalDateTime;

public class RoleListVO {

    private Long id;
    private String roleCode;
    private String roleName;
    /**
     * 由 {@code roleCode} 派生的 i18n key（见 {@code RoleI18nKeyResolver}），
     * 不落库，仅作 API 契约的一部分。前端按 {@code role.{roleCode 小写}} 查
     * {@code locales/{lang}/role.json}，查不到（自建角色）回退显示 roleName。
     */
    private String i18nKey;
    private Integer dataScope;
    private Integer userCount;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getI18nKey() { return i18nKey; }
    public void setI18nKey(String i18nKey) { this.i18nKey = i18nKey; }
    public Integer getDataScope() { return dataScope; }
    public void setDataScope(Integer dataScope) { this.dataScope = dataScope; }
    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
