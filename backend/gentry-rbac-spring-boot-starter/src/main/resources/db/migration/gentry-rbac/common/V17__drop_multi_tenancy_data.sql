-- =============================================
-- V17 拿掉多租户 · 第一步：角色与菜单数据清理
--
-- 详见 doc/design/modules/core/去多租户化-概要设计.md
--
-- 只做 DML，三库通用。结构性改动（DROP COLUMN/DROP TABLE）拆到 V18（纯портable
-- 部分）与 V19（需要先处理索引、按方言分文件）—— 这一步先把依赖这些角色/菜单行的
-- 数据理顺，后面两步删列删表时才不会留下悬空引用。
-- =============================================

-- ---------------------------------------------
-- 1. SUPER_ADMIN 并入 ADMIN
--
-- 去掉多租户后，"平台超管能跨租户、租户管理员不能"这条区分理由不存在了——
-- 系统里只该有一种"最高权限"。决策：保留 ADMIN 这个编码（不保留 SUPER_ADMIN，
-- "超级"这个修饰词描述的正是"高于其他租户"，语义不再准确）。
--
-- 先把 SUPER_ADMIN 名下的用户关联迁到 ADMIN（若该用户已经是 ADMIN 则不重复插入，
-- 避免 (user_id, role_id) 撞出重复行），再删掉所有仍指向 SUPER_ADMIN 的关联行、
-- 删掉 SUPER_ADMIN 自己的菜单授权、最后删掉这个角色本身。
-- ---------------------------------------------
-- MySQL 不允许 UPDATE 的子查询直接引用同一张表（ERROR 1093），
-- 用"子查询套一层派生表"绕开——MySQL 会把派生表物化，PostgreSQL/SQLite 同样支持这个写法。
UPDATE sys_user_role SET role_id = 1
WHERE role_id = -1
  AND user_id NOT IN (SELECT user_id FROM (SELECT user_id FROM sys_user_role WHERE role_id = 1) AS already_admin);

DELETE FROM sys_user_role WHERE role_id = -1;
DELETE FROM sys_role_menu WHERE role_id = -1;
DELETE FROM sys_role WHERE id = -1;

-- ---------------------------------------------
-- 2. 删除租户管理菜单（目录 101 + 其下 6 个按钮）
--
-- 这套权限点存在的唯一理由是"管理租户本身"，租户机制不存在了，它们也没有
-- 存在的意义。同时删掉授权关联，避免留下指向已删菜单的悬空 sys_role_menu 行。
-- ---------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (101, 1011, 1012, 1013, 1014, 1015, 1016);
DELETE FROM sys_menu WHERE id IN (101, 1011, 1012, 1013, 1014, 1015, 1016);

-- ---------------------------------------------
-- 3. 补发 ADMIN 缺的菜单：现在只有一种管理员，理应拥有全部（剩余）菜单
--
-- 有两组权限点此前只发给了 SUPER_ADMIN、从没发给 ADMIN：
--   ① 菜单管理本体（system:menu / :add / :edit / :remove）—— V14/V15 引入 is_platform
--      机制时，把这几条已发给 ADMIN 的授权当作越权收回了（那时"平台级"还有意义）
--   ② Redis 破坏性操作（monitor:redis:key:delete / slowlog:reset）—— V2 种子数据
--      本来就只给了 SUPER_ADMIN
-- 合并成单一角色后，"这条权限该不该给管理员"这个问题的答案统一变成"该给"——
-- 不存在需要限制的下级角色了。用 NOT EXISTS 只补缺的，不会对已有授权产生重复行。
-- ---------------------------------------------
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT 70000 + m.id, 1, m.id, CURRENT_TIMESTAMP
FROM sys_menu m
WHERE m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
