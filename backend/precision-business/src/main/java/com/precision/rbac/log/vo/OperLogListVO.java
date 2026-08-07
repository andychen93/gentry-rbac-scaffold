package com.precision.rbac.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperLogListVO {
    private Long id;
    private String module;
    private String moduleLabel;
    private String type;
    private String typeLabel;
    private String title;
    private String operator;
    private String operatorIp;
    private String location;
    private Integer status;
    private Integer costTime;
    private LocalDateTime operateTime;
}
