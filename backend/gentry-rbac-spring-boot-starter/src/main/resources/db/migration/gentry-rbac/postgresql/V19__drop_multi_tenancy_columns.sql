-- =============================================
-- V19 拿掉多租户 · 第三步：删 tenant_id 列（PostgreSQL 方言）
--
-- 详见 doc/design/modules/core/去多租户化-概要设计.md
--
-- 与 mysql/V19、sqlite/V19 是同一份改动的三份方言实现，字段和索引语义必须保持一致。
-- PostgreSQL 用局部唯一索引（WHERE deleted = 0），删列前先 DROP INDEX，
-- 删完列再按新列集合重建。
-- =============================================

-- sys_user
DROP INDEX IF EXISTS uk_user_tenant_username;
DROP INDEX IF EXISTS uk_user_tenant_phone;
DROP INDEX IF EXISTS idx_user_tenant_dept;
ALTER TABLE sys_user DROP COLUMN tenant_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_username ON sys_user(username) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_phone ON sys_user(phone) WHERE phone IS NOT NULL AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_user_dept ON sys_user(dept_id);

-- sys_role
DROP INDEX IF EXISTS uk_role_tenant_code;
ALTER TABLE sys_role DROP COLUMN tenant_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_code ON sys_role(role_code) WHERE deleted = 0;

-- sys_user_role：tenant_id 列本身没有专属索引，直接删列
ALTER TABLE sys_user_role DROP COLUMN tenant_id;

-- sys_dept
DROP INDEX IF EXISTS idx_dept_tenant_parent;
ALTER TABLE sys_dept DROP COLUMN tenant_id;
CREATE INDEX IF NOT EXISTS idx_dept_parent ON sys_dept(parent_id);

-- sys_dict_type / sys_dict_data：tenant_id 列本身没有专属索引，直接删列
ALTER TABLE sys_dict_type DROP COLUMN tenant_id;
ALTER TABLE sys_dict_data DROP COLUMN tenant_id;

-- sys_oper_log
DROP INDEX IF EXISTS idx_oper_log_tenant_time;
ALTER TABLE sys_oper_log DROP COLUMN tenant_id;
CREATE INDEX IF NOT EXISTS idx_oper_log_time ON sys_oper_log(operate_time);

-- sys_login_log
DROP INDEX IF EXISTS idx_login_log_tenant_time;
ALTER TABLE sys_login_log DROP COLUMN tenant_id;
CREATE INDEX IF NOT EXISTS idx_login_log_time ON sys_login_log(login_time);

-- sys_notification（V11 新增）
DROP INDEX IF EXISTS idx_notification_tenant_user_read;
DROP INDEX IF EXISTS idx_notification_biz_ref;
ALTER TABLE sys_notification DROP COLUMN tenant_id;
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON sys_notification(user_id, read_status);
CREATE INDEX IF NOT EXISTS idx_notification_biz_ref ON sys_notification(biz_ref);

-- sys_email_token（V16 新增）
DROP INDEX IF EXISTS idx_email_token_purpose;
ALTER TABLE sys_email_token DROP COLUMN tenant_id;
CREATE INDEX IF NOT EXISTS idx_email_token_purpose ON sys_email_token(email, purpose);
