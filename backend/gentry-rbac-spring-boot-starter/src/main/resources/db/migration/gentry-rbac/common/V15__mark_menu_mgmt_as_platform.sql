-- =============================================
-- V15 菜单管理收归平台级
--
-- 承接 V14 建立的 sys_menu.is_platform 机制，处理 V14 里记为「待定」的那一项。
-- 详见 doc/design/modules/rbac/modules/平台级权限隔离/详细设计.md
--
-- 判断依据：sys_menu 是**全局表**（无 tenant_id，在 GentryTenantManager.IGNORE_TABLES
-- 里），菜单不做多租户。既然一份菜单树被所有租户共用，租户管理员改它就会影响其他
-- 租户 —— 按「租户下的最高权限」这条定义，菜单的写操作不属于租户级。
--
-- 种子数据原本把 system:menu:add/edit/remove 给了 ADMIN(role_id=1)，
-- 那是 V14 之前没有平台级/租户级之分时的遗留，不是有意的产品决策。
--
-- 三库通用：纯 UPDATE / DELETE，MySQL 8 / PostgreSQL 14 / SQLite 3.35+ 语法一致。
-- =============================================

-- ---------------------------------------------
-- 1. 标记：菜单管理页 + 三个写权限点
--
-- **`system:menu:list` 刻意不在此列**，这是本迁移唯一需要小心的地方：
-- MenuController 的树查询与详情用的是 `system:menu:list`，而前端
-- 「角色管理 → 权限」页（PermissionPage）要靠 `GET /api/v1/menus` 拉整棵菜单树
-- 才能画出勾选框。把 list 一起收走，租户管理员就再也分配不了任何权限 ——
-- 那是把一个越权缺陷换成一个功能缺陷。
--
-- 于是切法是：树能读（分配权限必需），页面进不去、内容改不了。
--   system:menu        (id=104,  type=2) 菜单项本身 → 收走，侧边栏不再出现「菜单管理」
--   system:menu:add    (id=1042, type=3) 写 → 收走
--   system:menu:edit   (id=1043, type=3) 写 → 收走
--   system:menu:remove (id=1044, type=3) 写 → 收走
--   system:menu:list   (id=1041, type=3) 读 → **保留**
--
-- 注意 `system:menu` 不是任何 @SaCheckPermission 的串（接口守卫全部落在
-- `:list`/`:add`/`:edit`/`:remove` 上），它纯粹决定这个页面在侧边栏是否可见。
-- 跟 V14 处理 `system:tenant` 目录项的方式一致。
-- ---------------------------------------------
UPDATE sys_menu SET is_platform = 1
WHERE permission IN (
    'system:menu',
    'system:menu:add',
    'system:menu:edit',
    'system:menu:remove'
);

-- ---------------------------------------------
-- 2. 收回已发出的授权
--
-- 同 V14：只改标记不清历史数据，洞还开着 —— 种子 ADMIN 以及每个已建租户的
-- ADMIN 角色现在都还握着这四条。
--
-- 判据仍是 role_code <> 'SUPER_ADMIN' 而非 role_id：派生项目的平台超管角色
-- id 未必是 -1。
-- ---------------------------------------------
DELETE FROM sys_role_menu
WHERE menu_id IN (SELECT id FROM sys_menu WHERE is_platform = 1)
  AND role_id IN (SELECT id FROM sys_role WHERE role_code <> 'SUPER_ADMIN');
