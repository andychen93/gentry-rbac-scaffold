package com.precision.rbac.tenant.vo;

import java.time.LocalDateTime;
import java.util.Map;

public class TenantDetailVO {

    private Long id;
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String logo;
    private String domain;
    private LocalDateTime expireTime;
    private Integer accountLimit;
    private Integer deviceLimit;
    private Integer status;
    private String remark;
    private Map<String, Object> config;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private TenantStatistics statistics;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public Integer getAccountLimit() { return accountLimit; }
    public void setAccountLimit(Integer accountLimit) { this.accountLimit = accountLimit; }
    public Integer getDeviceLimit() { return deviceLimit; }
    public void setDeviceLimit(Integer deviceLimit) { this.deviceLimit = deviceLimit; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public TenantStatistics getStatistics() { return statistics; }
    public void setStatistics(TenantStatistics statistics) { this.statistics = statistics; }
}
