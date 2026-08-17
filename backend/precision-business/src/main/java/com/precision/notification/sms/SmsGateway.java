package com.precision.notification.sms;

import java.util.List;

/**
 * 短信网关抽象。
 *
 * <p>脚手架不绑定具体短信服务商；接入真实网关（阿里云/腾讯云等）时实现本接口并注册为 Bean 即可，
 * 默认提供 {@link LoggingSmsGateway} 桩实现（见 {@code NotificationConfig}）。</p>
 */
public interface SmsGateway {

    /**
     * 发送短信。
     *
     * @param recipients 接收手机号列表（可空/为空表示无接收人，应安全跳过）
     * @param content    短信内容
     * @return 实际发送的条数
     */
    int send(List<String> recipients, String content);
}
