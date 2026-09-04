package com.gentry.rbac.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DictTypeUpdateDTO {
    @NotBlank(message = "{valid.dict.dictName.notBlank}")
    @Size(min = 2, max = 100, message = "{valid.dict.dictName.size}")
    private String dictName;
    private Integer status;
    @Size(max = 500, message = "{valid.common.remark.size}")
    private String remark;

    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
