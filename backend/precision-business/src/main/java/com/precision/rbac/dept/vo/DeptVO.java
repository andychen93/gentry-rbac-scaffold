package com.precision.rbac.dept.vo;

import java.time.LocalDateTime;

/**
 * 部门 VO（单条记录）
 */
public class DeptVO {

    private Long id;
    private Long parentId;
    private String ancestors;
    private String name;
    private Long leaderId;
    private String leaderName;
    private String phone;
    private String email;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getAncestors() { return ancestors; }
    public void setAncestors(String ancestors) { this.ancestors = ancestors; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
