-- V20: 补齐 sys_user_role 审计列
-- 背景：UserRole 实体继承 BaseEntity（createBy/createTime/updateBy/updateTime/deleted），
--       但 V1 建表 DDL 仅含 id/tenant_id/user_id/role_id/create_time，
--       导致注册分配默认角色时 INSERT 报 Unknown column 'create_by'。
--       （RoleMenu 未继承 BaseEntity，sys_role_menu 不受影响，无需处理。）

ALTER TABLE sys_user_role ADD COLUMN create_by   BIGINT      NULL;
ALTER TABLE sys_user_role ADD COLUMN update_by   BIGINT      NULL;
ALTER TABLE sys_user_role ADD COLUMN update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE sys_user_role ADD COLUMN deleted     SMALLINT    NOT NULL DEFAULT 0;
