package com.gentry.rbac.log.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体，映射 sys_oper_log 表。
 * 不继承 BaseEntity（日志表无 createTime/updateTime/deleted 等审计字段）。
 */
@Data
@Table("sys_oper_log")
public class OperLog {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String module;
    private String type;
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
