package com.gentry.notification.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.gentry.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内通知。{@code userId} 为空表示全员广播。
 *
 * <p>继承 {@link BaseEntity}，公共字段由 AutoFillHandler 自动处理。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_notification")
public class Notification extends BaseEntity {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /** 目标用户；null = 全员广播 */
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
