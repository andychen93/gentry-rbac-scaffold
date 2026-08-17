---
inclusion: fileMatch
fileMatchPattern: '**/*.sql'
---

# 数据库迁移约定（改 SQL 时生效）

## 本脚手架支持三种数据库

MySQL（默认）/ PostgreSQL / SQLite，用 Spring Profile 切换，一套业务代码不用改。
连接配置在 `application-{mysql,postgresql,sqlite}.yml`，切换方法见 README「切换数据库」。

## 唯一权威位置

```
backend/gentry-start/src/main/resources/db/migration/
├── common/       # 三库通用迁移（99% 的新迁移都放这里）
├── mysql/        # 仅 MySQL 需要方言的迁移
├── postgresql/   # 仅 PostgreSQL 需要方言的迁移
└── sqlite/       # 仅 SQLite 需要方言的迁移
```

`spring.flyway.locations` 按 profile 拼成
`classpath:db/migration/common,classpath:db/migration/{profile}`，
Flyway 按版本号统一排序执行，不管迁移物理上在哪个子目录。
`sql/reference/` 下的全量脚本只是历史快照，不参与构建。

## 铁律

- **已发布的迁移文件不可修改**（Flyway 校验 checksum，改了直接启动失败），
  变更一律追加新的 `V{n}__xxx.sql`
- 版本号连续递增，当前 common/ 最大是 `V5`，三个厂商目录都还只有 `V1`（建表）
- 建表用 `CREATE TABLE IF NOT EXISTS`，建索引用 `CREATE INDEX IF NOT EXISTS`
  （MySQL 的 `CREATE INDEX` 不支持 `IF NOT EXISTS`，只在建表语句内联 `KEY`/`UNIQUE KEY` 即可，
  见 mysql/V1 的写法）

## 新迁移放哪个目录？

**默认放 `common/`。** 只有真的碰到方言差异时才分别写三份到 `mysql/` `postgresql/` `sqlite/`，
常见触发点：

| 场景 | 处理 |
|------|------|
| 纯 DML（INSERT/UPDATE 菜单、权限、字典数据） | 放 `common/`，三库语法一致 |
| 建表、加索引、改列类型 | 先看下面的兼容写法表，能兼容就放 `common/` |
| 局部唯一索引 `WHERE deleted = 0` | MySQL 不支持，写成 `(col, deleted)` 组合唯一键；
  或者分别写 mysql/ 用组合键、postgresql+sqlite/ 用局部索引 |
| JSON 类型列 | 别用 `JSONB`/`JSON` 原生类型，Java 侧就是裸 `String` 就用 `TEXT`，三库通用 |

## 三库都兼容的写法（已实测验证，直接放 common/）

| 需求 | 写法 |
|------|------|
| 当前时间 | `CURRENT_TIMESTAMP`（**不要用 `NOW()`**，SQLite 不认这个函数名） |
| 分页 | `LIMIT n OFFSET m` |
| 字符串拼接 | `CONCAT(a, b, c)` |
| 列别名加引号 | 双引号 `AS "camelCaseName"`（三库都当标识符解析，不是字符串） |
| 逻辑删除 | `deleted SMALLINT NOT NULL DEFAULT 0` |
| 主键 | `id BIGINT NOT NULL PRIMARY KEY`（应用层雪花 ID 写入，不依赖 `AUTO_INCREMENT`/`SERIAL`） |

## 三库不兼容、需要分方言的写法

| 需求 | MySQL | PostgreSQL | SQLite |
|------|-------|------------|--------|
| 局部唯一索引 | 不支持 → 组合唯一键 `(col, deleted)` | `CREATE UNIQUE INDEX ... WHERE deleted = 0` | 同 PostgreSQL |
| 建表引擎/字符集 | `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4` | 不需要 | 不需要 |
| `CREATE INDEX IF NOT EXISTS` | 不支持，去掉 `IF NOT EXISTS`（迁移只跑一次，语义等价） | 支持 | 支持 |

## 表结构规范

- 命名：`{module}_{entity}`（业务）｜ `sys_{entity}`（系统）｜ `{e1}_{e2}_rel`（关联）｜ `{entity}_log`（日志）
- 必填字段：`id BIGINT`（雪花）、`create_time`、`update_time`、`deleted SMALLINT DEFAULT 0`
- 非全局表必须有 `tenant_id BIGINT NOT NULL`
- 逻辑删除下的唯一索引要带 `WHERE deleted = 0`（PG/SQLite）或把 `deleted` 纳入索引列（MySQL）
- 纯追加日志表不要 `deleted`

## 加权限点必须配套

后端加了 `@SaCheckPermission("biz:order:add")`，就要在 `common/` 里同一条迁移中插入菜单和角色关联
（纯 DML，三库通用）：

```sql
INSERT INTO sys_menu (id, parent_id, name, type, sort, permission)
VALUES (5011, 501, '新增', 3, 2, 'biz:order:add');

INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id = 5011;   -- SUPER_ADMIN
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (60000 + id), 1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id = 5011;    -- ADMIN
```

`sys_role_menu.id` 沿用 `50000 + menu_id`（SUPER_ADMIN）/ `60000 + menu_id`（ADMIN）
的约定，避免主键冲突。菜单 `type`：`1`=目录、`2`=页面、`3`=按钮。
`type=2` 的菜单必须填 `path` + `component`，且 `component` 要在
`frontend/src/utils/menuMapper.ts` 的 `COMPONENT_MAP` 里有对应项。

## 如果只想支持一种数据库

删掉不用的两个厂商目录（比如只保留 `common/` + `postgresql/`），
`application.yml` 的 `spring.profiles.active` 改成固定值，
删掉 `gentry-start/pom.xml` 里不需要的驱动依赖（`mysql-connector-j` / `postgresql` /
`sqlite-jdbc` / `flyway-mysql`，flyway-core 内置支持 PostgreSQL 和 SQLite）。
