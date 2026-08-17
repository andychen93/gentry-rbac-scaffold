package com.gentry.rbac.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DictTypeCreateDTO {
    @NotBlank(message = "{valid.dict.dictName.notBlank}")
    @Size(min = 2, max = 100, message = "{valid.dict.dictName.size}")
    private String dictName;

    @NotBlank(message = "{valid.dict.dictType.notBlank}")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,99}$", message = "{valid.dict.dictType.pattern}")
    private String dictType;

    private Integer status;
    @Size(max = 500, message = "{valid.common.remark.size}")
    private String remark;

    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
