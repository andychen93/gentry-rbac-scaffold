package com.gentry.rbac.menu.dto;

/**
 * 菜单查询 DTO
 */
public class MenuQueryDTO {

    private String name;
    private Integer status;
    private Integer type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
}
