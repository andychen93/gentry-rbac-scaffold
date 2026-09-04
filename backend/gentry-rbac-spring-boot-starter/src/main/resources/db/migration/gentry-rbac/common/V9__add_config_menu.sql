-- V9: 参数配置菜单（挂「系统管理」目录 id=1 下，菜单 id=107）+ 按钮（1071~1075）

INSERT INTO sys_menu (id, parent_id, name, icon, type, sort, path, component, permission) VALUES
    (107, 1, '参数配置', 'SettingOutlined', 2, 7, '/system/config', 'pages/config/ConfigPage', 'system:config');

INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1071, 107, '查询',     3, 1, 'system:config:list'),
    (1072, 107, '新增',     3, 2, 'system:config:add'),
    (1073, 107, '编辑',     3, 3, 'system:config:edit'),
    (1074, 107, '删除',     3, 4, 'system:config:remove'),
    (1075, 107, '刷新缓存', 3, 5, 'system:config:refresh');

-- 授权：SUPER_ADMIN(role_id=-1, 50000+段) 与 ADMIN(role_id=1, 60000+段)
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 50000 + id, -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (107, 1071, 1072, 1073, 1074, 1075);

INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 60000 + id, 1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (107, 1071, 1072, 1073, 1074, 1075);
