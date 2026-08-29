package com.gentry.rbac.menu.dto;

import jakarta.validation.constraints.*;

/**
 * 新增菜单 DTO
 */
public class MenuCreateDTO {

    private Long parentId;

    @NotBlank(message = "{valid.menu.name.notBlank}")
    @Size(min = 2, max = 50, message = "{valid.menu.name.size}")
    private String name;

    @Size(max = 100, message = "{valid.menu.icon.size}")
    private String icon;

    @NotNull(message = "{valid.menu.type.notNull}")
    private Integer type;

    @NotNull(message = "{valid.common.sort.notNull}")
    @Min(value = 0, message = "{valid.common.sort.min}")
    @Max(value = 999, message = "{valid.common.sort.max}")
    private Integer sort;

    private String permission;
    private String path;
    private String component;
    private Integer visible;
    private Integer status;
    private Integer isExternal;
    private Integer isCache;

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
}
