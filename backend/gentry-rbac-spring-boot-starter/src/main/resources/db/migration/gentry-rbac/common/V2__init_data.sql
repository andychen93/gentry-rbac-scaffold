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
