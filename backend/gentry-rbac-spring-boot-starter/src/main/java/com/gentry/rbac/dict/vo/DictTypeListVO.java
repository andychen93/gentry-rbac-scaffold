package com.gentry.rbac.dict.vo;

import java.time.LocalDateTime;

public class DictTypeListVO {
    private Long id;
    private String dictName;
    /** i18n key，由 dict_type 派生（dict.type.*），可为 null。不落库，属 API 契约。 */
    private String i18nKey;
    private String dictType;
    private Integer dataCount;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getI18nKey() { return i18nKey; }
    public void setI18nKey(String i18nKey) { this.i18nKey = i18nKey; }
    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public Integer getDataCount() { return dataCount; }
    public void setDataCount(Integer dataCount) { this.dataCount = dataCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
