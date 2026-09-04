package com.gentry.notification.sms;

import java.util.List;

/**
 * 短信接收人解析。
 */
public interface SmsRecipientResolver {

    /**
     * 解析短信接收人手机号列表（默认实现取全部管理员）。
     *
     * @return 手机号列表，无接收人时返回空列表
     */
    List<String> resolve();
}
