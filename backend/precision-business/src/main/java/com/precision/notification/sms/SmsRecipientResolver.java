package com.precision.notification.sms;

import java.util.List;

/**
 * 短信接收人解析。
 */
public interface SmsRecipientResolver {

    /**
     * 解析某租户的短信接收人手机号列表（默认实现取租户管理员）。
     *
     * @param tenantId 租户 ID
     * @return 手机号列表，无接收人时返回空列表
     */
    List<String> resolve(Long tenantId);
}
