-- =============================================
-- V19 拿掉多租户 · 第三步：删 tenant_id 列（MySQL 方言）
--
-- 详见 doc/design/modules/core/去多租户化-概要设计.md
--
-- 需要分方言的原因：涉及 tenant_id 的唯一索引在三库写法不同（MySQL 组合唯一键
-- vs PostgreSQL/SQLite 局部唯一索引），删列前必须先删掉引用该列的索引/键，
-- 删完列再按新的列集合重建。这份是 MySQL 方言，与 postgresql/V19、sqlite/V19
-- 是同一份改动的三份方言实现，字段和索引语义必须保持一致。
--
-- MySQL 允许一条 ALTER TABLE 里合并 DROP KEY + DROP COLUMN + ADD 新 KEY，
-- 这里按表拆成多条语句，读起来更清楚，行为等价。
-- =============================================

-- sys_user：uk_user_tenant_username / uk_user_tenant_phone / idx_user_tenant_dept 三个索引都含 tenant_id
ALTER TABLE sys_user DROP INDEX uk_user_tenant_username;
ALTER TABLE sys_user DROP INDEX uk_user_tenant_phone;
ALTER TABLE sys_user DROP INDEX idx_user_tenant_dept;
ALTER TABLE sys_user DROP COLUMN tenant_id;
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_username (username, deleted);
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_phone (phone, deleted);
ALTER TABLE sys_user ADD KEY idx_user_dept (dept_id);

-- sys_role：uk_role_tenant_code 含 tenant_id
ALTER TABLE sys_role DROP INDEX uk_role_tenant_code;
ALTER TABLE sys_role DROP COLUMN tenant_id;
ALTER TABLE sys_role ADD UNIQUE KEY uk_role_code (role_code, deleted);

-- sys_user_role：tenant_id 列本身没有专属索引，直接删列
ALTER TABLE sys_user_role DROP COLUMN tenant_id;

-- sys_dept：idx_dept_tenant_parent 含 tenant_id
ALTER TABLE sys_dept DROP INDEX idx_dept_tenant_parent;
ALTER TABLE sys_dept DROP COLUMN tenant_id;
ALTER TABLE sys_dept ADD KEY idx_dept_parent (parent_id);

-- sys_dict_type / sys_dict_data：tenant_id 列本身没有专属索引，直接删列
ALTER TABLE sys_dict_type DROP COLUMN tenant_id;
ALTER TABLE sys_dict_data DROP COLUMN tenant_id;

-- sys_oper_log：idx_oper_log_tenant_time 含 tenant_id
ALTER TABLE sys_oper_log DROP INDEX idx_oper_log_tenant_time;
ALTER TABLE sys_oper_log DROP COLUMN tenant_id;
ALTER TABLE sys_oper_log ADD KEY idx_oper_log_time (operate_time);

-- sys_login_log：idx_login_log_tenant_time 含 tenant_id
ALTER TABLE sys_login_log DROP INDEX idx_login_log_tenant_time;
ALTER TABLE sys_login_log DROP COLUMN tenant_id;
ALTER TABLE sys_login_log ADD KEY idx_login_log_time (login_time);

-- sys_notification（V11 新增）：idx_notification_tenant_user_read / idx_notification_biz_ref 含 tenant_id
ALTER TABLE sys_notification DROP INDEX idx_notification_tenant_user_read;
ALTER TABLE sys_notification DROP INDEX idx_notification_biz_ref;
ALTER TABLE sys_notification DROP COLUMN tenant_id;
ALTER TABLE sys_notification ADD KEY idx_notification_user_read (user_id, read_status);
ALTER TABLE sys_notification ADD KEY idx_notification_biz_ref (biz_ref);

-- sys_email_token（V16 新增）：idx_email_token_purpose 含 tenant_id
ALTER TABLE sys_email_token DROP INDEX idx_email_token_purpose;
ALTER TABLE sys_email_token DROP COLUMN tenant_id;
ALTER TABLE sys_email_token ADD KEY idx_email_token_purpose (email, purpose);
