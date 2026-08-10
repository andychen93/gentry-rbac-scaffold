package com.precision.rbac.config.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigUpdateDTO {
    /** configKey 不可修改（业务依赖键名） */
    @Size(max = 100) private String configName;
    @Size(max = 500) private String configValue;
    @Size(max = 1) private String configType;
    @Size(max = 500) private String remark;
}
