-- V16: 参数配置菜单去掉图标（用户要求：菜单项不带图标）
-- V9 种子写入 icon='SettingOutlined'，已发布迁移不可改，此处置空。
-- 前端 getIcon 对空 icon 返回 undefined，侧边栏/菜单管理均安全。

UPDATE sys_menu SET icon = NULL WHERE id = 107 AND name = '参数配置';
