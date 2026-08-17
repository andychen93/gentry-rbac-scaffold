package com.gentry.notification.push;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
 * 通知实时推送：按租户维护 SSE 连接，新通知生成时实时下发到该租户在线客户端。
 *
 * <p>前端通知铃铛订阅后无需轮询即可即时收到（轮询作为 SSE 失败时的兜底仍然保留）。</p>
 */
@Service
public class NotificationPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushService.class);
    /** 10 分钟：断连的 emitter 靠超时兜底清理，别设太长 */
    private static final long SSE_TIMEOUT = 10 * 60 * 1000L;
    private static final long HEARTBEAT_PERIOD_SECONDS = 10L;

    /** tenantId → emitters */
    private final ConcurrentMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "notification-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public NotificationPushService() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats, HEARTBEAT_PERIOD_SECONDS, HEARTBEAT_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    /** 订阅某租户的通知流。 */
    public SseEmitter subscribe(Long tenantId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitter.onCompletion(() -> remove(tenantId, emitter));
        emitter.onTimeout(() -> remove(tenantId, emitter));
        emitter.onError(e -> remove(tenantId, emitter));
        emitters.computeIfAbsent(tenantId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"ok\"}"));
        } catch (Exception e) {
            remove(tenantId, emitter);
        }
        return emitter;
    }

    /** 向某租户所有在线客户端推送通知。失败连接会被移除；无连接时为空操作。 */
    public void push(Long tenantId, Object payload) {
        if (tenantId == null) {
            return;
        }
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(tenantId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("通知 SSE 推送失败，移除 emitter: {}", e.getMessage());
                }
                remove(tenantId, emitter);
            }
        }
    }

    /**
     * 定时发送 SSE 注释心跳，避免代理在长时间无通知时关闭空闲连接，
     * 同时通过写失败及时清理断开的 emitter。
     */
    void sendHeartbeats() {
        emitters.forEach((tenantId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("hb"));
                } catch (Exception e) {
                    remove(tenantId, emitter);
                }
            }
        });
    }

    /** 某租户当前在线连接数（监控/测试用）。 */
    public int getEmitterCount(Long tenantId) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(tenantId);
        return list == null ? 0 : list.size();
    }

    private void remove(Long tenantId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(tenantId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(tenantId, list);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        heartbeatScheduler.shutdownNow();
        emitters.values().forEach(list -> list.forEach(SseEmitter::complete));
        emitters.clear();
    }
}
