-- =============================================
-- V14 标记平台级权限点，修租户管理员越权
--
-- 缺陷：新建租户的 ADMIN 角色拿到了**全部**菜单（含 system:tenant:*），
-- 于是任何租户的管理员都能列出/新增/修改/删除所有租户。
-- 详见 doc/design/modules/rbac/modules/平台级权限隔离/详细设计.md
--
-- 三库通用：ALTER TABLE ... ADD COLUMN 与 UPDATE 在
-- MySQL 8 / PostgreSQL 14 / SQLite 3.35+ 语法一致。
-- =============================================

-- ---------------------------------------------
-- 1. sys_menu 增加「是否平台级权限点」
--
-- 为什么把判据放在菜单表：sys_menu 本来就是权限点注册表，「这个权限点是平台级的」
-- 是权限点**自身**的属性，不是某个角色的属性。于是两个角色的定义可推导：
--     SUPER_ADMIN = 全部菜单
--     租户 ADMIN   = 全部 is_platform = 0 的菜单（= 租户下的最高权限）
--
-- 备选方案「复制种子 ADMIN(role_id=1) 的菜单集合当基线」已否决：那个角色能通过
-- 「角色管理 → 权限」界面改，等于让点几下鼠标改变所有未来租户的安全基线。
--
-- 默认 0：派生项目新增的菜单一律按租户级处理，要设成平台级得显式标记。
-- ---------------------------------------------
ALTER TABLE sys_menu ADD COLUMN is_platform SMALLINT NOT NULL DEFAULT 0;

-- ---------------------------------------------
-- 2. 标记平台级权限点
--
-- 取的就是种子数据里 ADMIN 已经没有的那 9 个，不另发明一套。
-- 背后是两个不同的理由，结论相同：
--
--   ① 租户管理 —— 管理租户本身，天然跨租户
--   ② Redis 的两个破坏性操作 —— Redis 是所有租户**共用一个实例**，
--      删 Key / 清慢日志会影响其他租户，属平台运维动作
--
-- 注意 Redis 只挡写：monitor:redis:info 与只读的 Key 查询仍留给租户管理员，
-- 这是种子数据里既有的意图（可以看共享基础设施，不能改它）。
-- ---------------------------------------------
UPDATE sys_menu SET is_platform = 1
WHERE permission IN (
    'system:tenant',
    'system:tenant:list',
    'system:tenant:add',
    'system:tenant:edit',
    'system:tenant:remove',
    'system:tenant:detail',
    'system:tenant:config',
    'monitor:redis:key:delete',
    'monitor:redis:slowlog:reset'
);

-- ---------------------------------------------
-- 3. 修正历史数据：把已存在的非超管角色身上的平台级权限收回
--
-- 缺陷已经在运行过的库里留下了越权授权（每个建过的租户都有一份）。
-- 只改代码不清历史数据，等于洞还开着。
--
-- 判据是「角色的 role_code 不是 SUPER_ADMIN」而不是 role_id —— 派生项目的
-- 平台超管角色 id 未必是 -1。
-- ---------------------------------------------
DELETE FROM sys_role_menu
WHERE menu_id IN (SELECT id FROM sys_menu WHERE is_platform = 1)
  AND role_id IN (SELECT id FROM sys_role WHERE role_code <> 'SUPER_ADMIN');
