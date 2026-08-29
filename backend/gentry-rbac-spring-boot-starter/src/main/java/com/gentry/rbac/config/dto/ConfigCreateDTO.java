package com.gentry.rbac.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigCreateDTO {
    @Size(max = 100) private String configName;
    @NotBlank(message = "{valid.config.configKey.notBlank}") @Size(max = 100) private String configKey;
    @Size(max = 500) private String configValue;
    /** Y=系统内置 N=业务自定义 */
    @Size(max = 1) private String configType;
    @Size(max = 500) private String remark;
}
