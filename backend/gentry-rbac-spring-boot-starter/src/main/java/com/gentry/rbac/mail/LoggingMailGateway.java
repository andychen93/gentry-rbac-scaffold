package com.gentry.rbac.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮件网关桩实现：仅记录日志，不真正发送。
 *
 * <p>由 {@code MailConfig} 在无其它 {@link MailGateway} Bean 时注册为默认实现；
 * {@code gentry.mail.enabled=true} 装配 {@link SmtpMailGateway} 后自动让位，
 * 消费方自定义网关同样以 Bean 覆盖。</p>
 */
public class LoggingMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("[MAIL-STUB] Simulated mail -> {}: {} (body {} chars)", to, subject, htmlBody.length());
    }
}
