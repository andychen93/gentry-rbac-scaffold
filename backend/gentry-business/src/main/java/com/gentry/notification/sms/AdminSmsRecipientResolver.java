package com.gentry.notification.sms;

import java.util.List;

import com.mybatisflex.core.tenant.TenantManager;
import com.gentry.notification.mapper.NotificationRecipientMapper;
import org.springframework.stereotype.Component;

/**
 * 默认短信接收人解析：取租户管理员手机号。
 *
 * <p>查询期间显式忽略租户拦截器条件，改由 SQL 里的 {@code tenant_id} 精确控制，
 * 以保证在无请求租户上下文的异步线程中也能解析到正确租户的接收人。</p>
 */
@Component
public class AdminSmsRecipientResolver implements SmsRecipientResolver {

    private final NotificationRecipientMapper recipientMapper;

    public AdminSmsRecipientResolver(NotificationRecipientMapper recipientMapper) {
        this.recipientMapper = recipientMapper;
    }

    @Override
    public List<String> resolve(Long tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        TenantManager.ignoreTenantCondition();
        try {
            List<String> phones = recipientMapper.selectAdminPhones(tenantId);
            return phones == null ? List.of() : phones;
        } finally {
            TenantManager.restoreTenantCondition();
        }
    }
}
