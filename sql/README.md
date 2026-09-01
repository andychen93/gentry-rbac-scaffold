# sql/

## 唯一权威来源是 Flyway

数据库结构与初始化数据由 Flyway 管理，迁移分**双流**：

```
# 平台流（rbac-starter jar 携带，V1–V999 平台保留段）
backend/gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/
├── common/       # 三库通用迁移（V1–V19，目前用到这里）
├── mysql/        # 仅 MySQL 需要方言的迁移（V1 建表）
├── postgresql/   # 仅 PostgreSQL 需要方言的迁移（V1 建表）
└── sqlite/       # 仅 SQLite 需要方言的迁移（V1 建表）

# 项目流（狗粮 = gentry-start；消费项目 = 自己工程，V1000 起）
backend/gentry-start/src/main/resources/db/migration/
├── common/       # 三库通用迁移（目前为空，消费项目自己的业务迁移放这里）
├── mysql/
├── postgresql/
└── sqlite/
```

应用启动时按 `spring.profiles.active`（mysql，默认 / postgresql / sqlite）自动执行
`spring.flyway.locations` 里配置的四个目录组合（两流各一个 common + 对应厂商目录）。
**不要**手工改已发布的迁移文件，新增变更一律追加 `V{n}__xxx.sql`；**项目侧禁止复制、
修改平台段迁移**。

三份方言 `V1__create_schema.sql` 字段和索引语义必须保持一致，改动规则和「哪些写法三库
通用/哪些要分方言」的判断表见 `.kiro/steering/database-migration.md`。详细的双流规则见
`AGENTS.md` 第七章。

## 只想留一种数据库？

删掉不用的两个厂商目录（**平台流和项目流两侧都删**），`application.yml` 的
`spring.profiles.active` 写死，`gentry-start/pom.xml` 里删掉不需要的驱动（`flyway-mysql`
只有用 MySQL 才需要，PostgreSQL/SQLite 是 `flyway-core` 内置支持）。

## reference/

之前这里放过一份一次性全量初始化快照（`rbac_full_init.sql`），用于「不想跑应用、只想
手动灌一个库看看」的场景。去多租户化改造（V17–V19）后该快照已严重过期（还是老的
`tenant_id` + `SUPER_ADMIN` schema），按本文件上一节的规则已删除。**以 Flyway 为唯一权威**：
真要手动灌库，跑应用一次让 Flyway 自动迁移即可，不要再维护脱离 Flyway 的静态快照。
