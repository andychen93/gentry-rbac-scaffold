package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UserQueryDTO {

    @Min(value = 1, message = "{valid.common.pageNum.min}")
    private Integer pageNum = 1;

    @Min(value = 1, message = "{valid.common.pageSize.min}")
    @Max(value = 100, message = "{valid.common.pageSize.max}")
    private Integer pageSize = 10;

    private Long deptId;
    private String username;
    /** 昵称模糊查询（用户下拉选择器按昵称搜索用） */
    private String nickname;
    private String phone;
    private Integer status;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public int getOffset() { return (pageNum - 1) * pageSize; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
