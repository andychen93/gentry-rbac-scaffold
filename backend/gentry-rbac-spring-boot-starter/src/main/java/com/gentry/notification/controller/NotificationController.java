package com.gentry.notification.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.gentry.core.common.PageResult;
import com.gentry.core.common.R;
import com.gentry.core.security.UserContext;
import com.gentry.notification.dto.NotificationPublishDTO;
import com.gentry.notification.entity.Notification;
import com.gentry.notification.sender.NotificationSender;
import com.gentry.notification.service.NotificationService;
import com.gentry.rbac.log.annotation.Log;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 站内通知接口：当前用户的通知列表、未读数、标记已读，以及管理员发布通知。
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSender notificationSender;

    public NotificationController(NotificationService notificationService,
                                  NotificationSender notificationSender) {
        this.notificationService = notificationService;
        this.notificationSender = notificationSender;
    }

    @GetMapping
    @SaCheckPermission("notice:list")
    public R<PageResult<Notification>> list(
            @RequestParam(required = false) Integer readStatus,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(notificationService.listMine(readStatus, type, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    @SaCheckPermission("notice:list")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    @SaCheckPermission("notice:list")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    @PutMapping("/read-all")
    @SaCheckPermission("notice:list")
    public R<Integer> markAllRead() {
        return R.ok(notificationService.markAllRead());
    }

    /**
     * 发布通知（广播或定向）。
     *
     * <p>源项目里通知只由车辆报警事件产生，脚手架没有那套事件，
     * 这里补一个显式发布接口，让「系统公告」这类场景可用，也便于验证
     * 落库 → SSE 推送 → 铃铛 的整条链路。</p>
     */
    @PostMapping
    @SaCheckPermission("notice:publish")
    @Log(module = "消息通知", type = "INSERT", title = "发布通知")
    public R<Notification> publish(@Valid @RequestBody NotificationPublishDTO dto) {
        Notification n = new Notification();
        n.setTenantId(UserContext.getTenantId());
        n.setUserId(dto.getUserId());
        n.setType(dto.getType() == null || dto.getType().isBlank() ? "SYSTEM" : dto.getType());
        n.setLevel(dto.getLevel());
        n.setTitle(dto.getTitle());
        n.setContent(dto.getContent());
        n.setBizRef(dto.getBizRef());
        return R.ok(notificationSender.send(n));
    }
}
