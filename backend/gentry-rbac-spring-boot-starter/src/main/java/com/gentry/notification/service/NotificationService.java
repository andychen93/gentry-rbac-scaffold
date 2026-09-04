package com.gentry.notification.service;

import com.gentry.core.common.PageResult;
import com.gentry.notification.entity.Notification;

public interface NotificationService {

    /**
     * 落库一条站内通知。
     *
     * <p>业务代码一般不直接调这个，用 {@code NotificationSender.send(...)}
     * 才会同时触发 SSE 推送与高级别短信。</p>
     */
    Notification create(Notification notification);

    /** 当前用户可见的通知分页（user_id 为空的广播 + 本人定向）。 */
    PageResult<Notification> listMine(Integer readStatus, String type, int pageNum, int pageSize);

    /** 当前用户未读数量。 */
    long unreadCount();

    /** 标记单条已读（仅限当前用户可见的通知）。 */
    void markRead(Long id);

    /** 标记当前用户全部未读为已读。 */
    int markAllRead();
}
