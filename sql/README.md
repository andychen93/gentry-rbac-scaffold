# sql/

## 唯一权威来源是 Flyway

数据库结构与初始化数据由 Flyway 管理，脚本在：

```
backend/precision-start/src/main/resources/db/migration/
├── V1__create_schema.sql          # 12 张 sys_* 表
├── V2__init_data.sql              # 默认租户 + 菜单/权限点 + 角色 + 部门 + 用户 + 字典
├── V3__init_monitor_redis.sql     # 监控中心目录 + Redis 监控菜单与权限
├── V4__monitor_redis_slowlog.sql  # Redis 慢日志清空权限
└── V5__add_home_menu.sql          # 工作台首页菜单
```

应用启动时自动执行（`spring.flyway.enabled=true`，`baseline-on-migrate=true`）。
**不要**手工改已发布的迁移文件，新增变更一律追加 `V{n}__xxx.sql`。

## reference/

`reference/rbac_full_init.sql` 是把 V1+V2 拼成的一次性全量初始化脚本，仅用于
「不想跑应用、只想手动灌一个库看看」的场景：

```bash
psql -h localhost -U postgres -d precision -f sql/reference/rbac_full_init.sql
```

它不参与构建，也不被 Flyway 读取。**如果你改了 V1/V2，这份快照就过期了** —— 要么同步更新，
要么直接删掉它，以 Flyway 为准。
