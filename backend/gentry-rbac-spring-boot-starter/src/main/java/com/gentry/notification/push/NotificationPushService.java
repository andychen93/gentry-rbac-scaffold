package com.gentry.notification.push;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通知实时推送：维护全部在线 SSE 连接，新通知生成时广播给所有在线客户端。
 *
 * <p>前端通知铃铛订阅后无需轮询即可即时收到（轮询作为 SSE 失败时的兜底仍然保留）。</p>
 *
 * <p>本仓库已拿掉多租户机制（见 {@code doc/design/modules/core/去多租户化-概要设计.md}），
 * 此前按 tenantId 分组维护多套连接列表的设计已简化为单一全局列表——不再存在
 * 需要互相隔离的多个组织。</p>
 */
@Service
public class NotificationPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushService.class);
    /** 10 分钟：断连的 emitter 靠超时兜底清理，别设太长 */
    private static final long SSE_TIMEOUT = 10 * 60 * 1000L;
    private static final long HEARTBEAT_PERIOD_SECONDS = 10L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "notification-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public NotificationPushService() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats, HEARTBEAT_PERIOD_SECONDS, HEARTBEAT_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    /** 订阅通知流。 */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitter.onCompletion(() -> remove(emitter));
        emitter.onTimeout(() -> remove(emitter));
        emitter.onError(e -> remove(emitter));
        emitters.add(emitter);
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"ok\"}"));
        } catch (Exception e) {
            remove(emitter);
        }
        return emitter;
    }

    /** 向所有在线客户端推送通知。失败连接会被移除；无连接时为空操作。 */
    public void push(Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Notification SSE push failed, removing emitter: {}", e.getMessage());
                }
                remove(emitter);
            }
        }
    }

    /**
     * 定时发送 SSE 注释心跳，避免代理在长时间无通知时关闭空闲连接，
     * 同时通过写失败及时清理断开的 emitter。
     */
    void sendHeartbeats() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("hb"));
            } catch (Exception e) {
                remove(emitter);
            }
        }
    }

    /** 当前在线连接数（监控/测试用）。 */
    public int getEmitterCount() {
        return emitters.size();
    }

    private void remove(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    @PreDestroy
    public void destroy() {
        heartbeatScheduler.shutdownNow();
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }
}
