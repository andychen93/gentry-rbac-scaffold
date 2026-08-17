package com.gentry.rbac.menu.dto;

import jakarta.validation.constraints.*;

/**
 * 编辑菜单 DTO（type 不可修改，不含 type 字段）
 */
public class MenuUpdateDTO {

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 2, max = 50, message = "菜单名称长度为2-50字符")
    private String name;

    @Size(max = 100, message = "图标长度不能超过100字符")
    private String icon;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序值最小为0")
    @Max(value = 999, message = "排序值最大为999")
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
