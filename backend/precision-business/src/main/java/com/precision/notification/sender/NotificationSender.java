package com.precision.notification.sender;

import com.precision.notification.entity.Notification;
import com.precision.notification.push.NotificationPushService;
import com.precision.notification.service.NotificationService;
import com.precision.notification.sms.SmsGateway;
import com.precision.notification.sms.SmsRecipientResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知发送入口：落库 → SSE 实时推送 →（高级别）短信。
 *
 * <p>业务代码要发通知一律走这里，不要直接调 {@code NotificationService.create}，
 * 否则前端铃铛拿不到实时推送、也不会触发短信。</p>
 *
 * <h3>级别路由</h3>
 * <table>
 *   <tr><th>级别</th><th>渠道</th></tr>
 *   <tr><td>1 紧急 / 2 严重</td><td>站内信 + SSE 推送 + 短信</td></tr>
 *   <tr><td>3 一般 / 4 提示</td><td>站内信 + SSE 推送</td></tr>
 * </table>
 *
 * <p>与源项目的差异：源项目里这套路由绑在 {@code AlarmCreatedEvent}（车辆报警）上，
 * 且 3/4 级「仅记录不通知」——因为报警本身已另有台账。本脚手架里通知就是台账，
 * 故所有级别都落库并推送，只有 ≤2 级额外发短信。</p>
 */
@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    /** 达到该级别（数值≤）才发短信 */
    private static final int SMS_LEVEL_THRESHOLD = 2;

    private final NotificationService notificationService;
    private final NotificationPushService pushService;
    private final SmsRecipientResolver recipientResolver;
    private final SmsGateway smsGateway;

    public NotificationSender(NotificationService notificationService,
                              NotificationPushService pushService,
                              SmsRecipientResolver recipientResolver,
                              SmsGateway smsGateway) {
        this.notificationService = notificationService;
        this.pushService = pushService;
        this.recipientResolver = recipientResolver;
        this.smsGateway = smsGateway;
    }

    /**
     * 发送通知。tenantId 必填（异步线程无上下文时也要能定位租户）。
     *
     * <p>推送与短信的异常一律吞掉并记日志：通知是旁路能力，
     * 不能因为下发失败把调用方的业务主流程带崩。</p>
     *
     * @return 已落库的通知（含生成的 id）
     */
    public Notification send(Notification notification) {
        if (notification == null || notification.getTenantId() == null) {
            throw new IllegalArgumentException("通知的 tenantId 不能为空");
        }
        int level = notification.getLevel() == null ? 3 : notification.getLevel();
        boolean withSms = level <= SMS_LEVEL_THRESHOLD;
        notification.setLevel(level);
        notification.setChannels(withSms ? "INAPP,SMS" : "INAPP");

        Notification saved = notificationService.create(notification);

        try {
            pushService.push(saved.getTenantId(), saved);
        } catch (Exception e) {
            log.warn("通知 SSE 推送失败: id={}, {}", saved.getId(), e.getMessage());
        }

        if (withSms) {
            try {
                List<String> phones = recipientResolver.resolve(saved.getTenantId());
                if (phones.isEmpty()) {
                    log.warn("高级别通知无短信接收人: tenantId={}, title={}",
                            saved.getTenantId(), saved.getTitle());
                } else {
                    smsGateway.send(phones, buildSmsContent(saved));
                }
            } catch (Exception e) {
                log.error("通知短信下发失败: id={}", saved.getId(), e);
            }
        }
        return saved;
    }

    private String buildSmsContent(Notification n) {
        String content = n.getContent() == null ? "" : n.getContent();
        return "【" + n.getTitle() + "】" + content;
    }
}
