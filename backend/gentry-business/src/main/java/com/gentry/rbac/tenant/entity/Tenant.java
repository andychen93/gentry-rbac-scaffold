package com.gentry.rbac.tenant.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户实体（全局表，无 tenant_id）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_tenant")
public class Tenant extends BaseEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String logo;
    private String domain;
    private Long packageId;
    private LocalDateTime expireTime;
    private Integer accountLimit;
    private Integer status;
    private String remark;
    /**
     * 租户扩展配置，JSON 字符串，Service 层自行解析。列类型是 TEXT（三库通用），不是 JSONB。
     *
     * <p>字段清单以 {@code TenantConfigDTO} 为准，写入是**白名单覆盖**而非增量合并 ——
     * 否则删掉一个配置项之后，历史 JSON 里的旧键会永久留着并继续下发给前端。</p>
     */
    private String config;
    // createBy, createTime, updateBy, updateTime, deleted 由 BaseEntity 基类提供
}
