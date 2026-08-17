import React, { useCallback, useEffect, useState } from 'react';
import { Badge, Button, Popover, List, Tag, Empty, Spin, Typography, message, theme } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { notificationApi } from '../../services/notificationApi';
import type { NotificationVO } from '../../services/notificationApi';
import { useUserStore } from '../../stores/userStore';

/** 级别 → 展示文案与 Tag 配色（Tag 用 antd 预设色名，跟随主题） */
const LEVEL_META: Record<number, { text: string; color: string }> = {
  1: { text: '紧急', color: 'red' },
  2: { text: '严重', color: 'orange' },
  3: { text: '一般', color: 'blue' },
  4: { text: '提示', color: 'default' },
};

/** SSE 断开时的兜底轮询周期 */
const POLL_INTERVAL_MS = 30_000;
/** 弹层里最多展示多少条 */
const LIST_SIZE = 10;

/**
 * 顶栏通知铃铛：SSE 实时推送 + 轮询兜底，支持标记已读/全部已读。
 *
 * <p>无 notice:list 权限时整个铃铛不渲染，避免打出一串 403。</p>
 */
const NotificationBell: React.FC = () => {
  const { token } = theme.useToken();
  const hasPermission = useUserStore((s) => s.hasPermission);
  const canRead = hasPermission('notice:list');

  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<NotificationVO[]>([]);

  const loadUnread = useCallback(async () => {
    try {
      const res = await notificationApi.unreadCount();
      setUnread(Number(res.data ?? 0));
    } catch {
      // 静默：未授权或网络问题不打扰用户
    }
  }, []);

  const loadList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await notificationApi.list({ pageNum: 1, pageSize: LIST_SIZE });
      setItems(res.data?.list ?? []);
    } catch {
      /* 忽略 */
    } finally {
      setLoading(false);
    }
  }, []);

  // 轮询兜底
  useEffect(() => {
    if (!canRead) return;
    loadUnread();
    const timer = setInterval(loadUnread, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [canRead, loadUnread]);

  /*
   * SSE 实时推送：新通知即时更新未读数并前插列表。
   *
   * 用手动指数退避重连，而不是依赖浏览器 EventSource 的自动重连 ——
   * 后端 5xx 或鉴权失败时浏览器会无限快速重连，造成句柄泄漏和日志刷屏。
   * token 只能走 query 参数（EventSource 不能设请求头），后端已把
   * /api/v1/notifications/stream 从 Sa-Token 拦截器排除并自行校验。
   */
  useEffect(() => {
    if (!canRead) return;
    const authToken = localStorage.getItem('precision_token');
    if (!authToken) return;
    if (typeof EventSource === 'undefined') return; // jsdom 等环境无 EventSource

    const url = `/api/v1/notifications/stream?token=${encodeURIComponent(authToken)}`;
    const INITIAL_BACKOFF_MS = 3000;
    const MAX_BACKOFF_MS = 30000;
    const MAX_RETRIES = 10;

    let source: EventSource | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    let retryCount = 0;
    let closed = false;

    const clearTimer = () => {
      if (reconnectTimer !== null) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
    };
    const teardown = () => {
      if (source) {
        try {
          source.close();
        } catch {
          /* ignore */
        }
        source = null;
      }
    };

    const handleMessage = (evt: MessageEvent) => {
      try {
        const item = JSON.parse(evt.data) as NotificationVO;
        setUnread((c) => c + 1);
        setItems((prev) =>
          prev.some((n) => String(n.id) === String(item.id))
            ? prev
            : [item, ...prev].slice(0, 20),
        );
        const meta = LEVEL_META[item.level] ?? LEVEL_META[3];
        if (item.level <= 2) {
          message.warning(`[${meta.text}] ${item.title}`);
        }
      } catch {
        /* 忽略异常载荷 */
      }
    };

    const connect = () => {
      if (closed) return;
      clearTimer();
      teardown();
      const es = new EventSource(url);
      source = es;
      es.addEventListener('notification', handleMessage as EventListener);
      es.onopen = () => {
        retryCount = 0;
      };
      es.onerror = () => {
        teardown();
        if (retryCount >= MAX_RETRIES) return;
        const delay = Math.min(INITIAL_BACKOFF_MS * 2 ** retryCount, MAX_BACKOFF_MS);
        retryCount += 1;
        reconnectTimer = setTimeout(() => {
          reconnectTimer = null;
          connect();
        }, delay);
      };
    };

    const handleOffline = () => {
      clearTimer();
      teardown();
    };
    const handleOnlineOrVisible = () => {
      if (closed) return;
      if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return;
      if (source) return;
      retryCount = 0;
      clearTimer();
      connect();
    };

    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnlineOrVisible);
    document.addEventListener('visibilitychange', handleOnlineOrVisible);

    connect();

    return () => {
      closed = true;
      clearTimer();
      teardown();
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnlineOrVisible);
      document.removeEventListener('visibilitychange', handleOnlineOrVisible);
    };
  }, [canRead]);

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (next) loadList();
  };

  const handleMarkRead = async (item: NotificationVO) => {
    if (item.readStatus === 1) return;
    try {
      await notificationApi.markRead(item.id);
      setItems((prev) =>
        prev.map((n) => (String(n.id) === String(item.id) ? { ...n, readStatus: 1 } : n)),
      );
      loadUnread();
    } catch {
      /* 忽略 */
    }
  };

  const handleMarkAll = async () => {
    try {
      await notificationApi.markAllRead();
      setItems((prev) => prev.map((n) => ({ ...n, readStatus: 1 })));
      setUnread(0);
      message.success('已全部标记为已读');
    } catch {
      /* 忽略 */
    }
  };

  if (!canRead) return null;

  const content = (
    <div style={{ width: 360 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span style={{ fontWeight: 600 }}>通知</span>
        <Button type="link" size="small" onClick={handleMarkAll} disabled={unread === 0}>
          全部已读
        </Button>
      </div>
      <Spin spinning={loading}>
        {items.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无通知" />
        ) : (
          <List
            size="small"
            dataSource={items}
            style={{ maxHeight: 360, overflow: 'auto' }}
            renderItem={(item) => {
              const meta = LEVEL_META[item.level] ?? LEVEL_META[3];
              return (
                <List.Item
                  style={{
                    cursor: 'pointer',
                    // 未读用主色浅底标记（不写死 hex，跟随 argonColors）
                    background: item.readStatus === 0 ? token.controlItemBgActive : undefined,
                  }}
                  onClick={() => handleMarkRead(item)}
                >
                  <List.Item.Meta
                    title={
                      <span>
                        <Tag color={meta.color}>{meta.text}</Tag>
                        {item.title}
                      </span>
                    }
                    description={
                      <div style={{ fontSize: 12 }}>
                        <Typography.Text type="secondary">{item.content}</Typography.Text>
                        {/* createTime 由后端统一格式化，无需 dayjs */}
                        <div>
                          <Typography.Text type="secondary">{item.createTime ?? ''}</Typography.Text>
                        </div>
                      </div>
                    }
                  />
                </List.Item>
              );
            }}
          />
        )}
      </Spin>
    </div>
  );

  return (
    <Popover content={content} trigger="click" open={open} onOpenChange={handleOpenChange} placement="bottomRight">
      <Badge count={unread} size="small" offset={[-2, 2]}>
        <Button type="text" aria-label="通知" icon={<BellOutlined style={{ fontSize: 18 }} />} />
      </Badge>
    </Popover>
  );
};

export default React.memo(NotificationBell);
