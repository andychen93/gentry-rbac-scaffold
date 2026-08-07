package com.precision.rbac.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DictTypeCreateDTO {
    @NotBlank(message = "字典名称不能为空")
    @Size(min = 2, max = 100, message = "字典名称2-100字符")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{1,99}$", message = "字母开头，字母数字下划线，2-100字符")
    private String dictType;

    private Integer status;
    @Size(max = 500, message = "备注最长500字符")
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
