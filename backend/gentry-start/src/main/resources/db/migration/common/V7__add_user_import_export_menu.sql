-- V7: 用户管理新增「导出 / 导入」按钮权限
-- V2 已占用用户管理按钮 1021~1026（list/add/edit/remove/resetPwd/assignRole），本迁移用 1027/1028。

INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1027, 102, '导出', 3, 7, 'system:user:export'),
    (1028, 102, '导入', 3, 8, 'system:user:import');

-- 授权：SUPER_ADMIN(role_id=-1, 50000+ 段) 与 ADMIN(role_id=1, 60000+ 段)
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time) VALUES
    (50127, -1, 1027, CURRENT_TIMESTAMP),
    (50128, -1, 1028, CURRENT_TIMESTAMP),
    (60127, 1,  1027, CURRENT_TIMESTAMP),
    (60128, 1,  1028, CURRENT_TIMESTAMP);
