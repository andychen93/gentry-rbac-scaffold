package com.gentry.rbac.mail;

/**
 * 邮件网关抽象。
 *
 * <p>对齐 {@code SmsGateway} 的扩展模式：脚手架不绑定具体邮件服务商，
 * 默认提供 {@link LoggingMailGateway} 日志桩（开发/测试），生产 SMTP 用
 * {@link SmtpMailGateway}（{@code gentry.mail.enabled=true} 时装配）。
 * 消费方要接企业邮件 API 时，提供自己的 {@link MailGateway} Bean 即自动覆盖。</p>
 */
public interface MailGateway {

    /**
     * 发送 HTML 邮件。
     *
     * @param to        收件人邮箱（已规范化为小写）
     * @param subject   邮件主题（已本地化）
     * @param htmlBody  HTML 正文
     */
    void send(String to, String subject, String htmlBody);
}
