package com.gentry.rbac.user.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class User extends TenantEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String email;
    private Integer gender;
    private String postName;
    private String avatar;
    private Long deptId;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateTime;
    private String remark;
    /**
     * 语言偏好，三态：
     * {@code null} = 从未选择过（跟随浏览器 Accept-Language）；
     * {@code zh_CN} / {@code en_US} = 显式选择过，固定。
     */
    private String language;
    // tenantId, createBy, createTime, updateBy, updateTime, deleted 由 TenantEntity 基类提供
}
