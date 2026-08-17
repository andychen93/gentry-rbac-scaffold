package com.precision.notification.sms;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 短信网关桩实现：仅记录日志，不真正发送。
 *
 * <p>由 {@code NotificationConfig} 在无其它 {@link SmsGateway} Bean 时注册为默认实现；
 * 接入真实网关时另行提供 Bean 即自动覆盖。</p>
 */
public class LoggingSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public int send(List<String> recipients, String content) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("[SMS-STUB] 无接收人，跳过短信发送: content={}", content);
            return 0;
        }
        log.info("[SMS-STUB] 模拟发送短信 -> {} 个接收人: {}", recipients.size(), content);
        return recipients.size();
    }
}
