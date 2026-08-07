-- =============================================
-- RBAC 权限脚手架 - 监控模块初始化（Redis 监控）
-- 说明: 新增监控相关菜单与权限
-- =============================================

-- 一级目录：监控中心（独立于"系统监控"之外，用于未来接入 Redis/JVM/Prometheus 等各类监控）
INSERT INTO sys_menu (id, parent_id, name, icon, type, sort, path) VALUES
    (3, 0, '监控中心', 'DashboardOutlined', 1, 3, '/monitor-center');

-- 二级菜单：Redis 监控
INSERT INTO sys_menu (id, parent_id, name, type, sort, path, component, permission) VALUES
    (301, 3, 'Redis 监控', 2, 1, '/monitor-center/redis', 'pages/monitor/RedisMonitorPage', 'monitor:redis:info');

-- 三级按钮：Redis Key CRUD 权限
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (3011, 301, 'Key 列表',  3, 1, 'monitor:redis:key:list'),
    (3012, 301, 'Key 详情',  3, 2, 'monitor:redis:key:query'),
    (3013, 301, 'Key 删除',  3, 3, 'monitor:redis:key:delete');

-- 关联：SUPER_ADMIN(-1) 拥有监控中心 + Redis 监控全部权限
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, NOW()
FROM sys_menu
WHERE id IN (3, 301, 3011, 3012, 3013);

-- 关联：ADMIN(1) 仅拥有查询类权限（不可删除 Key）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, NOW()
FROM sys_menu
WHERE id IN (3, 301, 3011, 3012);
