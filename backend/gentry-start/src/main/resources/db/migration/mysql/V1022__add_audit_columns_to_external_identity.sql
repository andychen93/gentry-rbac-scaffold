-- V1021 建表时遗漏 BaseEntity 期望的 create_by/update_by 审计列，
-- 导致 MyBatis-Flex 插入时报 "Unknown column 'create_by' in 'field list'"。
-- 已发布迁移不可修改，此处追加。
ALTER TABLE sys_external_identity
    ADD COLUMN create_by BIGINT NULL,
    ADD COLUMN update_by BIGINT NULL;
