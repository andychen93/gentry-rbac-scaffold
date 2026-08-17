package com.precision.notification.service.impl;

import java.util.List;

import com.mybatisflex.core.query.QueryWrapper;
import com.precision.core.common.ErrorCode;
import com.precision.core.common.PageResult;
import com.precision.core.exception.BizException;
import com.precision.core.security.UserContext;
import com.precision.notification.entity.Notification;
import com.precision.notification.mapper.NotificationMapper;
import com.precision.notification.service.NotificationService;
import org.springframework.stereotype.Service;

/**
 * 站内通知服务。
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Notification create(Notification notification) {
        if (notification.getReadStatus() == null) {
            notification.setReadStatus(0);
        }
        // 异步线程可能无租户上下文，按实体 tenantId 临时设置，确保 tenant_id 正确落库
        Long prev = UserContext.getTenantId();
        try {
            if (notification.getTenantId() != null) {
                UserContext.setTenantId(notification.getTenantId());
            }
            notificationMapper.insert(notification);
            return notification;
        } finally {
            UserContext.setTenantId(prev);
        }
    }

    @Override
    public PageResult<Notification> listMine(Integer readStatus, String type, int pageNum, int pageSize) {
        Long tenantId = UserContext.getTenantId();
        Long userId = UserContext.getUserId();

        QueryWrapper base = baseQuery(tenantId, userId, readStatus, type);
        long total = notificationMapper.selectCountByQuery(base);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, pageNum, pageSize);
        }
        QueryWrapper page = baseQuery(tenantId, userId, readStatus, type)
                .orderBy("create_time", false)
                .limit(pageSize).offset((pageNum - 1) * pageSize);
        List<Notification> list = notificationMapper.selectListByQuery(page);
        return new PageResult<>(list, total, pageNum, pageSize);
    }

    @Override
    public long unreadCount() {
        Long tenantId = UserContext.getTenantId();
        Long userId = UserContext.getUserId();
        return notificationMapper.selectCountByQuery(baseQuery(tenantId, userId, 0, null));
    }

    @Override
    public void markRead(Long id) {
        Long tenantId = UserContext.getTenantId();
        Long userId = UserContext.getUserId();
        Notification n = notificationMapper.selectOneById(id);
        if (n == null || !tenantId.equals(n.getTenantId()) || !visibleTo(n, userId)) {
            throw new BizException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (n.getReadStatus() != null && n.getReadStatus() == 1) {
            return;
        }
        n.setReadStatus(1);
        notificationMapper.update(n);
    }

    @Override
    public int markAllRead() {
        Long tenantId = UserContext.getTenantId();
        Long userId = UserContext.getUserId();
        Notification update = new Notification();
        update.setReadStatus(1);
        return notificationMapper.updateByQuery(update, baseQuery(tenantId, userId, 0, null));
    }

    /** 租户内 + (广播 user_id IS NULL 或 本人) + 可选 readStatus/type + 未删除。 */
    private QueryWrapper baseQuery(Long tenantId, Long userId, Integer readStatus, String type) {
        QueryWrapper qw = QueryWrapper.create()
                .where("tenant_id = ?", tenantId)
                .and("deleted = 0");
        if (userId != null) {
            qw.and("(user_id IS NULL OR user_id = ?)", userId);
        } else {
            qw.and("user_id IS NULL");
        }
        if (readStatus != null) {
            qw.and("read_status = ?", readStatus);
        }
        if (type != null && !type.isBlank()) {
            qw.and("type = ?", type);
        }
        return qw;
    }

    private boolean visibleTo(Notification n, Long userId) {
        return n.getUserId() == null || n.getUserId().equals(userId);
    }
}
