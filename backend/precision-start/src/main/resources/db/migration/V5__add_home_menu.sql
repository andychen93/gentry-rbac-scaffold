-- =============================================
-- 脚手架首页（工作台）
-- 说明: 登录后默认落地页。菜单 sort=0 保证排在最前，App.tsx 的 firstRoute 会取到它。
--       新项目若要换成自己的业务首页：
--         1) 新建 src/pages/xxx/XxxPage.tsx 并在 utils/menuMapper.ts 的 COMPONENT_MAP 登记；
--         2) 写一条新的 Flyway 迁移 UPDATE sys_menu SET component = 'pages/xxx/XxxPage' WHERE id = 4;
-- =============================================

-- 一级菜单：工作台（type=2 直接作为页面，无子菜单）
INSERT INTO sys_menu (id, parent_id, name, icon, type, sort, path, component, permission) VALUES
    (4, 0, '工作台', 'HomeOutlined', 2, 0, '/home', 'pages/home/HomePage', 'system:home');

-- 查询按钮权限（占位，便于后续细分首页卡片权限）
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (41, 4, '查看', 3, 1, 'system:home:view');

-- SUPER_ADMIN(-1) 与 ADMIN(1) 均可见
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, NOW() FROM sys_menu WHERE id IN (4, 41);

INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, NOW() FROM sys_menu WHERE id IN (4, 41);
