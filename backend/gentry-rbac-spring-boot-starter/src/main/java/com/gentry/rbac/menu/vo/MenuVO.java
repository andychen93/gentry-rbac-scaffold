package com.gentry.rbac.menu.vo;

import java.time.LocalDateTime;

/**
 * 菜单 VO（扁平，无 children）
 */
public class MenuVO {

    private Long id;
    private Long parentId;
    private String name;
    /**
     * i18n key，由 permission/path 派生，可为 null。
     *
     * <p>不落库：{@code sys_menu} 没有这一列，本字段是<b>API 契约</b>的一部分，
     * 由 {@code MenuI18nKeyResolver} 在组装 VO 时计算。前端渲染时
     * {@code label = t(i18nKey) ?? name}，为 null 或语言包缺 key 时回退 name。</p>
     */
    private String i18nKey;
    private String icon;
    private Integer type;
    private Integer sort;
    private String permission;
    private String path;
    private String component;
    private Integer visible;
    private Integer status;
    private Integer isExternal;
    private Integer isCache;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getI18nKey() { return i18nKey; }
    public void setI18nKey(String i18nKey) { this.i18nKey = i18nKey; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public Integer getVisible() { return visible; }
    public void setVisible(Integer visible) { this.visible = visible; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsExternal() { return isExternal; }
    public void setIsExternal(Integer isExternal) { this.isExternal = isExternal; }
    public Integer getIsCache() { return isCache; }
    public void setIsCache(Integer isCache) { this.isCache = isCache; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
