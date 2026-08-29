package com.gentry.rbac.config.dto;

import lombok.Data;

@Data
public class ConfigQueryDTO {
    private String configKey;
    private String configName;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
