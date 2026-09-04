package com.gentry.rbac.log.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体，映射 sys_login_log 表。
 * 不继承 BaseEntity（日志表无 createTime/updateTime/deleted 等审计字段）。
 */
@Data
@Table("sys_login_log")
public class LoginLog {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String username;
    private String loginType;
    private String loginIp;
    private String location;
    private String browser;
    private String os;
    private String deviceType;
    private String userAgent;
    private Integer status;
    private String message;
    private LocalDateTime loginTime;
}
