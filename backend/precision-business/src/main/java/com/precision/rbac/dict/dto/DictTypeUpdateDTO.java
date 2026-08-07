package com.precision.rbac.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DictTypeUpdateDTO {
    @NotBlank(message = "字典名称不能为空")
    @Size(min = 2, max = 100, message = "字典名称2-100字符")
    private String dictName;
    private Integer status;
    @Size(max = 500, message = "备注最长500字符")
    private String remark;

    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
