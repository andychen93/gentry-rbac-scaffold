# sql/

## 唯一权威来源是 Flyway

数据库结构与初始化数据由 Flyway 管理，脚本在：

```
backend/gentry-start/src/main/resources/db/migration/
├── common/                         # 三库通用迁移（版本号统一编号，与厂商目录穿插执行）
│   ├── V2__init_data.sql           # 默认租户 + 菜单/权限点 + 角色 + 部门 + 用户 + 字典
│   ├── V3__init_monitor_redis.sql  # 监控中心目录 + Redis 监控菜单与权限
│   ├── V4__monitor_redis_slowlog.sql  # Redis 慢日志清空权限
│   └── V5__add_home_menu.sql       # 工作台首页菜单
├── mysql/V1__create_schema.sql       # 12 张 sys_* 表（MySQL 方言）
├── postgresql/V1__create_schema.sql  # 同上（PostgreSQL 方言）
└── sqlite/V1__create_schema.sql      # 同上（SQLite 方言）
```

应用启动时按 `spring.profiles.active`（mysql，默认 / postgresql / sqlite）自动执行
对应组合：`classpath:db/migration/common,classpath:db/migration/{profile}`。
**不要**手工改已发布的迁移文件，新增变更一律追加 `V{n}__xxx.sql`。

三份 `V1__create_schema.sql` 字段和索引语义必须保持一致，改动规则和「哪些写法三库通用/
哪些要分方言」的判断表见 `.kiro/steering/database-migration.md`。

## 只想留一种数据库？

删掉不用的两个厂商目录，`application.yml` 的 `spring.profiles.active` 写死，
`gentry-start/pom.xml` 里删掉不需要的驱动（`flyway-mysql` 只有用 MySQL 才需要，
PostgreSQL/SQLite 是 `flyway-core` 内置支持）。

## reference/

`reference/rbac_full_init.sql` 是 PostgreSQL 版 `common/V2` 拼 `postgresql/V1` 的一次性全量
初始化脚本，仅用于「不想跑应用、只想手动灌一个库看看」的场景：

```bash
psql -h localhost -U postgres -d gentry -f sql/reference/rbac_full_init.sql
```

它不参与构建，也不被 Flyway 读取，且只覆盖 PostgreSQL 这一种方言。**如果你改了迁移文件，
这份快照就过期了** —— 要么同步更新，要么直接删掉它，以 Flyway 为准。
