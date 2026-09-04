-- V11: 站内通知表 + 权限点
--   源项目的 V23__create_notification.sql 是 PostgreSQL 专用写法（TIMESTAMPTZ / NOW()
--   / ON CONFLICT），这里改成三库通用：TIMESTAMP + CURRENT_TIMESTAMP，去掉 ON CONFLICT，
--   索引不用 IF NOT EXISTS（MySQL 的 CREATE INDEX 不支持）。
CREATE TABLE IF NOT EXISTS sys_notification (
    id           BIGINT        NOT NULL PRIMARY KEY,
    tenant_id    BIGINT        NOT NULL,
    user_id      BIGINT,                                   -- 目标用户；NULL = 租户内广播
    type         VARCHAR(30)   NOT NULL DEFAULT 'SYSTEM',   -- SYSTEM / 业务方自定义
    level        SMALLINT      NOT NULL DEFAULT 3,          -- 1紧急 2严重 3一般 4提示
    title        VARCHAR(200)  NOT NULL,
    content      VARCHAR(1000),
    biz_ref      VARCHAR(200),                              -- 关联业务键，便于回溯业务对象
    channels     VARCHAR(100),                              -- 实际下发渠道，如 "INAPP,SMS"
    read_status  SMALLINT      NOT NULL DEFAULT 0,          -- 0未读 1已读
    create_by    BIGINT,
    create_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT      NOT NULL DEFAULT 0
);

-- 铃铛拉取「未读 + 按时间倒序」，按此建复合索引
CREATE INDEX idx_notification_tenant_user_read ON sys_notification (tenant_id, user_id, read_status);
CREATE INDEX idx_notification_biz_ref ON sys_notification (tenant_id, biz_ref);

-- 权限点：挂「系统监控」目录(id=2)下的按钮。铃铛在顶栏、不是独立页面，故无 path/component
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (260, 2, '消息通知查询', 3, 9,  'notice:list'),
    (261, 2, '消息通知发布', 3, 10, 'notice:publish');

-- 授权：SUPER_ADMIN(role_id=-1, 50000+段) 与 ADMIN(role_id=1, 60000+段)
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 50000 + id, -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (260, 261);
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 60000 + id, 1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (260, 261);
