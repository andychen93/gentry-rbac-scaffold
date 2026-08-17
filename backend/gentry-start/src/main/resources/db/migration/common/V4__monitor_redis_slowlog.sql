-- =============================================
-- RBAC 权限脚手架 - Redis 监控增强：慢查询日志权限
-- =============================================

-- 新增按钮权限：慢查询清空（仅超管可操作）
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (3014, 301, '慢日志清空', 3, 4, 'monitor:redis:slowlog:reset');

-- 关联：SUPER_ADMIN 可清空慢日志
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 53014, -1, 3014, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM sys_menu WHERE id = 3014);
