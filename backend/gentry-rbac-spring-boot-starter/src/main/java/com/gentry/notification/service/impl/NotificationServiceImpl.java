package com.gentry.notification.service.impl;

import java.util.List;

import com.mybatisflex.core.query.QueryWrapper;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.UserContext;
import com.gentry.notification.entity.Notification;
import com.gentry.notification.mapper.NotificationMapper;
import com.gentry.notification.service.NotificationService;
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
        notificationMapper.insert(notification);
        return notification;
    }

    @Override
    public PageResult<Notification> listMine(Integer readStatus, String type, int pageNum, int pageSize) {
        Long userId = UserContext.getUserId();

        QueryWrapper base = baseQuery(userId, readStatus, type);
        long total = notificationMapper.selectCountByQuery(base);
        if (total == 0) {
            return new PageResult<>(List.of(), 0, pageNum, pageSize);
        }
        QueryWrapper page = baseQuery(userId, readStatus, type)
                .orderBy("create_time", false)
                .limit(pageSize).offset((pageNum - 1) * pageSize);
        List<Notification> list = notificationMapper.selectListByQuery(page);
        return new PageResult<>(list, total, pageNum, pageSize);
    }

    @Override
    public long unreadCount() {
        Long userId = UserContext.getUserId();
        return notificationMapper.selectCountByQuery(baseQuery(userId, 0, null));
    }

    @Override
    public void markRead(Long id) {
        Long userId = UserContext.getUserId();
        Notification n = notificationMapper.selectOneById(id);
        if (n == null || !visibleTo(n, userId)) {
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
        Long userId = UserContext.getUserId();
        Notification update = new Notification();
        update.setReadStatus(1);
        return notificationMapper.updateByQuery(update, baseQuery(userId, 0, null));
    }

    /** (广播 user_id IS NULL 或 本人) + 可选 readStatus/type + 未删除。 */
    private QueryWrapper baseQuery(Long userId, Integer readStatus, String type) {
        QueryWrapper qw = QueryWrapper.create()
                .where("deleted = 0");
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
