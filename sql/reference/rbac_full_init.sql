-- =============================================
-- RBAC 权限脚手架 - 完整初始化脚本（手动执行用，PostgreSQL 方言）
-- 使用: psql -h localhost -U postgres -d precision -f rbac_full_init.sql
--
-- 这是 postgresql/V1__create_schema.sql + common/V2~V5 拼接的快照，仅用于
-- "不想跑应用、只想手动灌一个库看看" 的场景，不参与构建、不被 Flyway 读取。
-- MySQL/SQLite 用户请直接跑应用，Flyway 会按 profile 自动建表初始化。
--
-- 账号:
--   chenli   / Chenli@2026  → SUPER_ADMIN（平台超管）
--   admin    / Abc@123456   → ADMIN（租户管理员）
--   zhangsan / Abc@123456   → ADMIN（租户管理员）
-- =============================================

-- =============================================
-- RBAC 权限脚手架 - 建表（PostgreSQL 14+）
-- =============================================

-- =============================================
-- 1. 租户表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_tenant (
    id              BIGINT       NOT NULL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    contact         VARCHAR(50),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    address         VARCHAR(200),
    logo            VARCHAR(500),
    domain          VARCHAR(200),
    package_id      BIGINT,
    expire_time     TIMESTAMP,
    account_limit   INT          NOT NULL DEFAULT 100,
    device_limit    INT          NOT NULL DEFAULT 1000,
    status          SMALLINT     NOT NULL DEFAULT 1,
    remark          VARCHAR(500),
    config          TEXT,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    create_by       BIGINT,
    update_by       BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_code ON sys_tenant(code) WHERE deleted = 0;

-- =============================================
-- 2. 用户表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT       NOT NULL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(200) NOT NULL,
    nickname        VARCHAR(50)  NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(100),
    gender          SMALLINT     NOT NULL DEFAULT 0,
    post_name       VARCHAR(50),
    avatar          VARCHAR(500),
    dept_id         BIGINT,
    status          SMALLINT     NOT NULL DEFAULT 1,
    login_ip        VARCHAR(50),
    login_date      TIMESTAMP,
    pwd_update_time TIMESTAMP,
    remark          VARCHAR(500),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    create_by       BIGINT,
    update_by       BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_tenant_username ON sys_user(tenant_id, username, deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_tenant_phone ON sys_user(tenant_id, phone) WHERE phone IS NOT NULL AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_tenant_dept ON sys_user(tenant_id, dept_id);

-- =============================================
-- 3. 角色表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT       NOT NULL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    role_code       VARCHAR(50)  NOT NULL,
    role_name       VARCHAR(50)  NOT NULL,
    data_scope      SMALLINT     NOT NULL DEFAULT 1,
    sort            INT          NOT NULL DEFAULT 0,
    status          SMALLINT     NOT NULL DEFAULT 1,
    remark          VARCHAR(500),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    create_by       BIGINT,
    update_by       BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_tenant_code ON sys_role(tenant_id, role_code, deleted);

-- =============================================
-- 4. 用户角色关联表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT    NOT NULL PRIMARY KEY,
    tenant_id   BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    role_id     BIGINT    NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_role_user ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role ON sys_user_role(role_id);

-- =============================================
-- 5. 菜单表（全局，无 tenant_id）
-- =============================================
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       NOT NULL PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(50)  NOT NULL,
    icon        VARCHAR(100),
    type        SMALLINT     NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    permission  VARCHAR(100),
    path        VARCHAR(200),
    component   VARCHAR(200),
    visible     SMALLINT     NOT NULL DEFAULT 1,
    status      SMALLINT     NOT NULL DEFAULT 1,
    is_external SMALLINT     NOT NULL DEFAULT 0,
    is_cache    SMALLINT     NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_menu_parent ON sys_menu(parent_id);

-- =============================================
-- 6. 角色菜单关联表（全局）
-- =============================================
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT    NOT NULL PRIMARY KEY,
    role_id     BIGINT    NOT NULL,
    menu_id     BIGINT    NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_role_menu_role ON sys_role_menu(role_id);

-- =============================================
-- 7. 角色部门关联表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_role_dept (
    id          BIGINT    NOT NULL PRIMARY KEY,
    role_id     BIGINT    NOT NULL,
    dept_id     BIGINT    NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- 8. 部门表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    ancestors   VARCHAR(500) NOT NULL DEFAULT '',
    name        VARCHAR(50)  NOT NULL,
    leader_id   BIGINT,
    leader_name VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    sort        INT          NOT NULL DEFAULT 0,
    status      SMALLINT     NOT NULL DEFAULT 1,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    create_by   BIGINT,
    update_by   BIGINT
);
CREATE INDEX IF NOT EXISTS idx_dept_tenant_parent ON sys_dept(tenant_id, parent_id);

-- =============================================
-- 9. 字典类型表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    dict_name   VARCHAR(100) NOT NULL,
    dict_type   VARCHAR(100) NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    create_by   BIGINT,
    update_by   BIGINT
);

-- =============================================
-- 10. 字典数据表
-- =============================================
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    dict_type   VARCHAR(100) NOT NULL,
    dict_label  VARCHAR(100) NOT NULL,
    dict_value  VARCHAR(100) NOT NULL,
    css_class   VARCHAR(100),
    list_class  VARCHAR(100),
    is_default  SMALLINT     NOT NULL DEFAULT 0,
    sort        INT          NOT NULL DEFAULT 0,
    status      SMALLINT     NOT NULL DEFAULT 1,
    remark      VARCHAR(500),
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0,
    create_by   BIGINT,
    update_by   BIGINT
);

-- =============================================
-- 11. 操作日志表（无逻辑删除）
-- =============================================
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id              BIGINT       NOT NULL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    module          VARCHAR(50)  NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    operator        VARCHAR(50)  NOT NULL,
    operator_id     BIGINT,
    operator_ip     VARCHAR(50)  NOT NULL,
    location        VARCHAR(100),
    method          VARCHAR(10),
    request_url     VARCHAR(500),
    request_params  TEXT,
    response_result TEXT,
    status          SMALLINT     NOT NULL,
    error_msg       TEXT,
    cost_time       INT          NOT NULL DEFAULT 0,
    operate_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_oper_log_tenant_time ON sys_oper_log(tenant_id, operate_time);

-- =============================================
-- 12. 登录日志表（无逻辑删除）
-- =============================================
CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    login_type  VARCHAR(20)  NOT NULL DEFAULT 'password',
    login_ip    VARCHAR(50)  NOT NULL,
    location    VARCHAR(100),
    browser     VARCHAR(50),
    os          VARCHAR(50),
    device_type VARCHAR(20),
    user_agent  VARCHAR(500),
    status      SMALLINT     NOT NULL,
    message     VARCHAR(200) NOT NULL,
    login_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_login_log_tenant_time ON sys_login_log(tenant_id, login_time);

-- =============================================
-- RBAC 权限脚手架 - 初始化数据
-- 说明: 默认租户 + 菜单 + 角色 + 部门 + 用户 + 字典
-- =============================================

-- =============================================
-- 1. 默认租户（平台基座，id=1，不可删除）
-- =============================================
INSERT INTO sys_tenant (id, code, name, contact, phone, status)
VALUES (1, 'default', '默认租户', '管理员', '13800138000', 1);

-- =============================================
-- 2. 菜单（全局，权限标识统一用 system: 前缀）
-- =============================================

-- 一级目录
INSERT INTO sys_menu (id, parent_id, name, icon, type, sort, path) VALUES
    (1, 0, '系统管理', 'SettingOutlined', 1, 1, '/system'),
    (2, 0, '系统监控', 'MonitorOutlined',  1, 2, '/monitor');

-- 二级菜单 - 系统管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, path, component, permission) VALUES
    (101, 1, '租户管理', 2, 1, '/system/tenants',  'pages/tenant/TenantPage',  'system:tenant'),
    (102, 1, '用户管理', 2, 2, '/system/users',    'pages/user/UserPage',      'system:user'),
    (103, 1, '角色管理', 2, 3, '/system/roles',    'pages/role/RolePage',      'system:role'),
    (104, 1, '菜单管理', 2, 4, '/system/menu',     'pages/menu/MenuPage',      'system:menu'),
    (105, 1, '部门管理', 2, 5, '/system/dept',     'pages/dept/DeptPage',      'system:dept'),
    (106, 1, '字典管理', 2, 6, '/system/dict',     'pages/dict/DictPage',      'system:dict');

-- 二级菜单 - 系统监控
INSERT INTO sys_menu (id, parent_id, name, type, sort, path, component, permission) VALUES
    (201, 2, '操作日志', 2, 1, '/monitor/operlog',  'pages/log/OperLogPage',    'system:operlog'),
    (202, 2, '登录日志', 2, 2, '/monitor/loginlog', 'pages/log/LoginLogPage',   'system:loginlog'),
    (203, 2, '在线用户', 2, 3, '/monitor/online',   'pages/log/OnlineUserPage', 'system:online');

-- 三级按钮 - 租户管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1011, 101, '查询', 3, 1, 'system:tenant:list'),
    (1012, 101, '新增', 3, 2, 'system:tenant:add'),
    (1013, 101, '编辑', 3, 3, 'system:tenant:edit'),
    (1014, 101, '删除', 3, 4, 'system:tenant:remove'),
    (1015, 101, '详情', 3, 5, 'system:tenant:detail');

-- 三级按钮 - 用户管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1021, 102, '查询',     3, 1, 'system:user:list'),
    (1022, 102, '新增',     3, 2, 'system:user:add'),
    (1023, 102, '编辑',     3, 3, 'system:user:edit'),
    (1024, 102, '删除',     3, 4, 'system:user:remove'),
    (1025, 102, '重置密码', 3, 5, 'system:user:resetPwd'),
    (1026, 102, '分配角色', 3, 6, 'system:user:assignRole');

-- 三级按钮 - 角色管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1031, 103, '查询',       3, 1, 'system:role:list'),
    (1032, 103, '新增',       3, 2, 'system:role:add'),
    (1033, 103, '编辑',       3, 3, 'system:role:edit'),
    (1034, 103, '删除',       3, 4, 'system:role:remove'),
    (1035, 103, '分配菜单',   3, 5, 'system:role:assignMenu'),
    (1036, 103, '数据权限',   3, 6, 'system:role:assignDataScope');

-- 三级按钮 - 菜单管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1041, 104, '查询', 3, 1, 'system:menu:list'),
    (1042, 104, '新增', 3, 2, 'system:menu:add'),
    (1043, 104, '编辑', 3, 3, 'system:menu:edit'),
    (1044, 104, '删除', 3, 4, 'system:menu:remove');

-- 三级按钮 - 部门管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1051, 105, '查询', 3, 1, 'system:dept:list'),
    (1052, 105, '新增', 3, 2, 'system:dept:add'),
    (1053, 105, '编辑', 3, 3, 'system:dept:edit'),
    (1054, 105, '删除', 3, 4, 'system:dept:remove');

-- 三级按钮 - 字典管理
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (1061, 106, '查询', 3, 1, 'system:dict:list'),
    (1062, 106, '新增', 3, 2, 'system:dict:add'),
    (1063, 106, '编辑', 3, 3, 'system:dict:edit'),
    (1064, 106, '删除', 3, 4, 'system:dict:remove');

-- 三级按钮 - 操作日志
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (2011, 201, '查询', 3, 1, 'system:operlog:list'),
    (2012, 201, '详情', 3, 2, 'system:operlog:detail'),
    (2013, 201, '删除', 3, 3, 'system:operlog:remove'),
    (2014, 201, '导出', 3, 4, 'system:operlog:export');

-- 三级按钮 - 登录日志
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (2021, 202, '查询', 3, 1, 'system:loginlog:list'),
    (2022, 202, '详情', 3, 2, 'system:loginlog:detail'),
    (2023, 202, '删除', 3, 3, 'system:loginlog:remove'),
    (2024, 202, '导出', 3, 4, 'system:loginlog:export');

-- 三级按钮 - 在线用户
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (2031, 203, '查询',     3, 1, 'system:online:list'),
    (2032, 203, '强制下线', 3, 2, 'system:online:forceLogout');

-- =============================================
-- 3. 部门（默认租户下）
-- =============================================
INSERT INTO sys_dept (id, tenant_id, parent_id, ancestors, name, sort, status, create_by, create_time, update_time, deleted) VALUES
    (100, 1, 0,   '0',     '总公司', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (101, 1, 100, '0,100', '技术部', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (102, 1, 100, '0,100', '运营部', 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- =============================================
-- 4. 角色
--    SUPER_ADMIN(id=-1): 平台超管，拥有全部菜单（含租户管理）
--    ADMIN(id=1): 租户管理员，除租户管理外全部菜单
-- =============================================
INSERT INTO sys_role (id, tenant_id, role_code, role_name, data_scope, status, sort, create_time, update_time, deleted) VALUES
    (-1, 1, 'SUPER_ADMIN', '超级管理员', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ( 1, 1, 'ADMIN',       '管理员',     1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- SUPER_ADMIN → 全部菜单
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE deleted = 0;

-- ADMIN → 除租户管理(101,1011~1015)外的全部菜单
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE deleted = 0 AND id NOT IN (101, 1011, 1012, 1013, 1014, 1015);

-- =============================================
-- 5. 用户
--    chenli:   超级管理员, 密码 Chenli@2026
--    admin:    管理员,     密码 Abc@123456
--    zhangsan: 管理员,     密码 Abc@123456
-- =============================================
INSERT INTO sys_user (id, tenant_id, username, password, nickname, gender, dept_id, post_name, status, create_time, update_time, deleted) VALUES
    (1, 1, 'chenli',
     '$2a$10$iN9f/DCndYzcik7gWYjmwOkHCH0Y6xAuhMOwjrCFlnJJ5IqLLsSVu',
     '陈立', 1, 100, '总架构师', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 1, 'admin',
     '$2a$10$/YTPKYX8uxAqSyH4XUqY3euW2S/ZWPTF2LdxDy9KQCP.ypHgSRkP.',
     '管理员', 1, 100, '经理', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3, 1, 'zhangsan',
     '$2a$10$/YTPKYX8uxAqSyH4XUqY3euW2S/ZWPTF2LdxDy9KQCP.ypHgSRkP.',
     '张三', 1, 101, '工程师', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- 用户角色关联
INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time) VALUES
    (1, 1, 1, -1, CURRENT_TIMESTAMP),   -- chenli   → SUPER_ADMIN
    (2, 1, 2,  1, CURRENT_TIMESTAMP),   -- admin    → ADMIN
    (3, 1, 3,  1, CURRENT_TIMESTAMP);   -- zhangsan → ADMIN

-- =============================================
-- 6. 字典数据
-- =============================================
INSERT INTO sys_dict_type (id, tenant_id, dict_name, dict_type, status) VALUES
    (1, 1, '用户性别',     'sys_user_gender',    1),
    (2, 1, '用户职务',     'sys_user_post',      1),
    (3, 1, '系统开关',     'sys_normal_disable',  1),
    (4, 1, '菜单类型',     'sys_menu_type',       1),
    (5, 1, '数据权限范围', 'sys_data_scope',      1);

INSERT INTO sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, css_class, sort, is_default, status) VALUES
    (1,  1, 'sys_user_gender',    '男',   '1', 'primary', 1, 0, 1),
    (2,  1, 'sys_user_gender',    '女',   '2', 'danger',  2, 0, 1),
    (3,  1, 'sys_user_gender',    '未知', '0', 'default', 3, 1, 1),
    (10, 1, 'sys_user_post', '首席执行官', 'CEO',            'primary', 1, 0, 1),
    (11, 1, 'sys_user_post', '总监',       'Director',       'primary', 2, 0, 1),
    (12, 1, 'sys_user_post', '经理',       'Manager',        'success', 3, 0, 1),
    (13, 1, 'sys_user_post', '主管',       'Supervisor',     'success', 4, 0, 1),
    (14, 1, 'sys_user_post', '总架构师',   'ChiefArchitect', 'primary', 5, 0, 1),
    (15, 1, 'sys_user_post', '高级工程师', 'SeniorEngineer', 'success', 6, 0, 1),
    (16, 1, 'sys_user_post', '工程师',     'Engineer',       'default', 7, 0, 1),
    (17, 1, 'sys_user_post', '司机',       'Driver',         'default', 8, 0, 1),
    (20, 1, 'sys_normal_disable', '正常', '1', 'success', 1, 1, 1),
    (21, 1, 'sys_normal_disable', '停用', '0', 'danger',  2, 0, 1),
    (40, 1, 'sys_menu_type', '目录', '1', 'primary', 1, 0, 1),
    (41, 1, 'sys_menu_type', '菜单', '2', 'success', 2, 0, 1),
    (42, 1, 'sys_menu_type', '按钮', '3', 'warning', 3, 0, 1),
    (50, 1, 'sys_data_scope', '全部数据',         '1', 'primary', 1, 0, 1),
    (51, 1, 'sys_data_scope', '本部门及子部门',   '2', 'success', 2, 0, 1),
    (52, 1, 'sys_data_scope', '本部门数据',       '3', 'warning', 3, 0, 1),
    (53, 1, 'sys_data_scope', '仅本人数据',       '4', 'danger',  4, 0, 1),
    (54, 1, 'sys_data_scope', '自定义',           '5', 'default', 5, 0, 1);

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
SELECT (50000 + id), -1, id, CURRENT_TIMESTAMP
FROM sys_menu
WHERE id IN (3, 301, 3011, 3012, 3013);

-- 关联：ADMIN(1) 仅拥有查询类权限（不可删除 Key）
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, CURRENT_TIMESTAMP
FROM sys_menu
WHERE id IN (3, 301, 3011, 3012);

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

-- =============================================
-- 脚手架首页（工作台）
-- 说明: 登录后默认落地页。菜单 sort=0 保证排在最前，App.tsx 的 firstRoute 会取到它。
-- =============================================

-- 一级菜单：工作台（type=2 直接作为页面，无子菜单）
INSERT INTO sys_menu (id, parent_id, name, icon, type, sort, path, component, permission) VALUES
    (4, 0, '工作台', 'HomeOutlined', 2, 0, '/home', 'pages/home/HomePage', 'system:home');

-- 查询按钮权限（占位，便于后续细分首页卡片权限）
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission) VALUES
    (41, 4, '查看', 3, 1, 'system:home:view');

-- SUPER_ADMIN(-1) 与 ADMIN(1) 均可见
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (4, 41);

INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id IN (4, 41);
