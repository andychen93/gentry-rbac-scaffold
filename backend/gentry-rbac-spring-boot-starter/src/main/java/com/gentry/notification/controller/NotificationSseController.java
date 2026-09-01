package com.gentry.notification.controller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.gentry.notification.push.NotificationPushService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通知实时推送 SSE 端点。
 *
 * <p>浏览器 EventSource 无法设置请求头，因此 token 走 query 参数并在此手动校验，
 * 不能依赖 Sa-Token 拦截器。</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationSseController {

    private final NotificationPushService pushService;

    public NotificationSseController(NotificationPushService pushService) {
        this.pushService = pushService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            throw NotLoginException.newInstance(StpUtil.getLoginType(),
                    NotLoginException.NOT_TOKEN, "未提供token", "");
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            throw NotLoginException.newInstance(StpUtil.getLoginType(),
                    NotLoginException.INVALID_TOKEN, "token无效", token);
        }
        return pushService.subscribe();
    }
}
