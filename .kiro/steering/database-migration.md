---
inclusion: fileMatch
fileMatchPattern: '**/*.sql'
---

# 数据库迁移约定（改 SQL 时生效）

## 唯一权威位置

```
backend/precision-start/src/main/resources/db/migration/V{n}__{说明}.sql
```

Flyway 在应用启动时自动执行。`sql/reference/` 下的全量脚本只是快照，不参与构建。

## 铁律

- **已发布的迁移文件不可修改**（Flyway 校验 checksum，改了直接启动失败），
  变更一律追加新的 `V{n}__xxx.sql`
- 版本号连续递增，当前最大是 `V5`
- 建表用 `CREATE TABLE IF NOT EXISTS`，建索引用 `CREATE INDEX IF NOT EXISTS`

## 表结构规范

- 命名：`{module}_{entity}`（业务）｜ `sys_{entity}`（系统）｜ `{e1}_{e2}_rel`（关联）｜ `{entity}_log`（日志）
- 必填字段：`id BIGINT`（雪花）、`create_time`、`update_time`、`deleted SMALLINT DEFAULT 0`
- 非全局表必须有 `tenant_id BIGINT NOT NULL`
- 逻辑删除下的唯一索引要带 `WHERE deleted = 0`，或把 `deleted` 纳入索引列
- 纯追加日志表不要 `deleted`

## 加权限点必须配套

后端加了 `@SaCheckPermission("biz:order:add")`，就要在同一条迁移里插入菜单和角色关联：

```sql
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission)
VALUES (5011, 501, '新增', 3, 2, 'biz:order:add');

INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, NOW() FROM sys_menu WHERE id = 5011;   -- SUPER_ADMIN
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, NOW() FROM sys_menu WHERE id = 5011;    -- ADMIN
```

`sys_role_menu.id` 沿用 `50000 + menu_id`（SUPER_ADMIN）/ `60000 + menu_id`（ADMIN）
的约定，避免主键冲突。菜单 `type`：`1`=目录、`2`=页面、`3`=按钮。
`type=2` 的菜单必须填 `path` + `component`，且 `component` 要在
`frontend/src/utils/menuMapper.ts` 的 `COMPONENT_MAP` 里有对应项。
