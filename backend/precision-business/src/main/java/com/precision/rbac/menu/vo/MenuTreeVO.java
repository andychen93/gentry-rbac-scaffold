package com.precision.rbac.menu.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单树 VO（含 children）
 */
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String name;
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
    private List<MenuTreeVO> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public List<MenuTreeVO> getChildren() { return children; }
    public void setChildren(List<MenuTreeVO> children) { this.children = children; }
}
