-- V6: 补「租户配置」权限点。
-- V2 遗漏了 system:tenant:config 菜单行，导致 PUT /api/v1/tenants/{id}/config 对所有角色 403
-- （V2 的「SUPER_ADMIN→全部菜单」是 SELECT 自 sys_menu，没有这行菜单就授不到这个权限）。
-- 三库通用：仅 INSERT，无方言差异；CURRENT_TIMESTAMP 在 MySQL/PostgreSQL/SQLite 均支持。

-- 挂到「租户管理」(101) 下，与 1011~1015 同级
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1016, 101, '配置', 3, 6, 'system:tenant:config');

-- 授予 SUPER_ADMIN（沿用 V2 的 50000+menu_id 编号；ADMIN 按设计不含租户菜单，故不授）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time) VALUES
    (51016, -1, 1016, CURRENT_TIMESTAMP);
