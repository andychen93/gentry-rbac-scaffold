-- =============================================
-- RBAC 权限脚手架 - 建表（SQLite 3.35+）
-- =============================================
-- 本文件与 mysql/V1、postgresql/V1 是同一份表结构的三份方言实现，
-- 字段和索引语义必须保持一致。改了这份记得同步另外两份，
-- 或者看 .kiro/steering/database-migration.md 里"何时需要分厂商目录"的判断标准。
--
-- SQLite 是动态类型（type affinity），下面的类型声明只影响存储亲和性，不做强校验；
-- 局部唯一索引（WHERE deleted = 0）SQLite 原生支持，写法与 PostgreSQL 一致。
--
-- 定位：SQLite 只用于本地体验 / 单元测试 / demo，不建议在多进程并发写场景下用于生产
-- （见 application-sqlite.yml 里 hikari 连接池被限制为 1 的说明）。

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
    -- 租户扩展配置：Java 侧是裸 String（Service 层自行 JSON 解析）
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
