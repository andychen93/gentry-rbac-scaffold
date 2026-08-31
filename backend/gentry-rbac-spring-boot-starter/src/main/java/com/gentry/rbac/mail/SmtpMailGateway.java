package com.gentry.rbac.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件网关：基于 Spring 标准的 {@code spring.mail.*} 配置。
 *
 * <p>开发环境对接 Mailpit（{@code spring.mail.host=localhost, port=1025}），
 * 生产参数由消费方以环境变量提供。{@code gentry.mail.enabled=false}（默认）
 * 时不装配，落到 {@link LoggingMailGateway} 日志桩。</p>
 */
@Component
@ConditionalOnProperty(prefix = "gentry.mail", name = "enabled", havingValue = "true")
public class SmtpMailGateway implements MailGateway {

    private final JavaMailSender mailSender;
    private final GentryMailProperties properties;

    public SmtpMailGateway(JavaMailSender mailSender, GentryMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(htmlBody);
        mailSender.send(message);
    }
}
