package com.precision.notification.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.precision.core.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内通知。{@code userId} 为空表示租户内广播。
 *
 * <p>继承 {@link TenantEntity}，tenant_id 与公共字段由拦截器/AutoFillHandler 自动处理。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_notification")
public class Notification extends TenantEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /** 目标用户；null = 租户内广播 */
    private Long userId;

    /** 类型：SYSTEM 系统通知 / 业务方自定义 */
    private String type;

    /** 级别：1紧急 2严重 3一般 4提示（≤2 会额外触发短信，见 NotificationSender） */
    private Integer level;

    private String title;

    private String content;

    /** 关联业务键，便于从通知回溯到业务对象 */
    private String bizRef;

    /** 实际下发渠道，如 "INAPP" / "INAPP,SMS" */
    private String channels;

    /** 0未读 1已读 */
    private Integer readStatus;
}
