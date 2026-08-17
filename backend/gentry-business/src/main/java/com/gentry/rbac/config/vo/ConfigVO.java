package com.gentry.rbac.config.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConfigVO {
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private String remark;
    private LocalDateTime createTime;
}
