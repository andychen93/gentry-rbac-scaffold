-- =============================================
-- V13 清理业务域残留
--
-- 本仓库派生自一个高精度车辆定位平台，改造成通用 RBAC 脚手架时有三处业务概念没清干净。
-- 详见 doc/design/modules/rbac/modules/业务域残留清理/详细设计.md
--
-- 三库通用：只用了 UPDATE / DELETE / ALTER TABLE ... DROP COLUMN，
-- 三者在 MySQL 8 / PostgreSQL 14 / SQLite 3.35+ 语法一致，故放在 common/ 而非各方言目录。
-- =============================================

-- ---------------------------------------------
-- 1. sys_user.post_name 由「中文 label」改存「sys_user_post 字典码」
--
-- 原来这一列存的是展示文案（「经理」），导致职务下拉的 value 必须是中文、
-- 因而无法翻译；改存码之后文案交给 dict.sys_user_post.{code} 译文。
--
-- ELSE post_name 是刻意的：认不出的值原样留下。派生项目可能已经往这列写了别的东西，
-- 静默清空是最坏的迁移行为。
--
-- V2__init_data.sql 已发布不可修改，其中种子用户写的仍是中文职务，
-- 由本次 UPDATE 一并转换 —— 全新安装是「V2 插中文 → V13 转码」，
-- 与增量升级的最终状态一致。
-- ---------------------------------------------
UPDATE sys_user
SET post_name = CASE post_name
    WHEN '首席执行官' THEN 'CEO'
    WHEN '总监'       THEN 'Director'
    WHEN '经理'       THEN 'Manager'
    WHEN '主管'       THEN 'Supervisor'
    WHEN '总架构师'   THEN 'ChiefArchitect'
    WHEN '高级工程师' THEN 'SeniorEngineer'
    WHEN '工程师'     THEN 'Engineer'
    WHEN '司机'       THEN 'Engineer'
    ELSE post_name
END
WHERE post_name IS NOT NULL;

-- ---------------------------------------------
-- 2. 删掉字典项「司机 / Driver」
--
-- RBAC 脚手架的内置职务清单里不该有「司机」。
--
-- 用物理 DELETE 而不是 deleted = 1：sys_dict_data 的唯一约束含 deleted，
-- 逻辑删除会让派生项目想重新添加 Driver 时撞上历史行。且这是一条从未被业务引用的
-- 种子数据（上一步已把可能存在的「司机」职务并入 Engineer），无引用完整性风险。
-- ---------------------------------------------
DELETE FROM sys_dict_data
WHERE dict_type = 'sys_user_post' AND dict_value = 'Driver';

-- ---------------------------------------------
-- 3. 租户配置去掉车辆监控字段
--
-- 3.1 config 是 TEXT 存 JSON，里面的
--     maxDevices / dataRetentionDays / features{video,alarm,report} / mapProvider
--     全是车辆监控概念。直接置 NULL 而不是逐键删：跨三库改 JSON 文本要三份方言
--     （JSON_REMOVE / jsonb - key / json_remove），而 V2 建内置租户时根本没写 config，
--     全新安装该列就是 NULL；唯一可能有内容的是「有人在配置弹窗点过保存」的开发库，
--     里面存的又恰好全是本次要删的键 —— 信息损失为零。
-- ---------------------------------------------
UPDATE sys_tenant SET config = NULL;

-- 3.2 device_limit 是真实列（NOT NULL DEFAULT 1000），同属车辆监控概念。
--     留着不用的成本是每个读这张表的人都要问一次「这列干什么的」。
--     该列不参与任何索引或唯一键，DROP COLUMN 在三库都安全。
ALTER TABLE sys_tenant DROP COLUMN device_limit;
