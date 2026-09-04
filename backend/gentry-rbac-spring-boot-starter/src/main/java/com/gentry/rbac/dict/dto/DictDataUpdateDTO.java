package com.gentry.rbac.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class DictDataUpdateDTO {
    @NotBlank(message = "{valid.dict.dictLabel.notBlank}")
    @Size(min = 1, max = 100)
    private String dictLabel;
    private String cssClass;
    private String listClass;
    private Integer isDefault;
    @Min(0) @Max(999)
    private Integer sort;
    private Integer status;
    @Size(max = 500)
    private String remark;

    public String getDictLabel() { return dictLabel; }
    public void setDictLabel(String dictLabel) { this.dictLabel = dictLabel; }
    public String getCssClass() { return cssClass; }
    public void setCssClass(String cssClass) { this.cssClass = cssClass; }
    public String getListClass() { return listClass; }
    public void setListClass(String listClass) { this.listClass = listClass; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
