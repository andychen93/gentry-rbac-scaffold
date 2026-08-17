package com.gentry.rbac.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperLogDetailVO {
    private Long id;
    private String module;
    private String moduleLabel;
    private String type;
    private String typeLabel;
    private String title;
    private String operator;
    private Long operatorId;
    private String operatorIp;
    private String location;
    private String method;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private Integer status;
    private String errorMsg;
    private Integer costTime;
    private LocalDateTime operateTime;
}
