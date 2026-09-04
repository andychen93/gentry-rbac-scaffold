package com.gentry.notification.sms;

import java.util.List;

import com.gentry.notification.mapper.NotificationRecipientMapper;
import org.springframework.stereotype.Component;

/**
 * 默认短信接收人解析：取全部管理员手机号。
 */
@Component
public class AdminSmsRecipientResolver implements SmsRecipientResolver {

    private final NotificationRecipientMapper recipientMapper;

    public AdminSmsRecipientResolver(NotificationRecipientMapper recipientMapper) {
        this.recipientMapper = recipientMapper;
    }

    @Override
    public List<String> resolve() {
        List<String> phones = recipientMapper.selectAdminPhones();
        return phones == null ? List.of() : phones;
    }
}
