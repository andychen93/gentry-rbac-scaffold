-- V10: 角色管理「绑定用户」按钮权限点（挂角色管理菜单 id=103 下，按钮 id=1037）
--   与既有 1031~1036 同段；配套接口 PUT /api/v1/roles/{id}/users
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1037, 103, '绑定用户', 3, 7, 'system:role:assignUser');

-- 授权：SUPER_ADMIN(role_id=-1, 50000+段) 与 ADMIN(role_id=1, 60000+段)
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 50000 + id, -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (1037);
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 60000 + id, 1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (1037);
