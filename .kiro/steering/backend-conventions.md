---
inclusion: fileMatch
fileMatchPattern: 'backend/**/*.java'
---

# 后端编码约定（改 Java 时生效）

权威约定在 [AGENTS.md](../../AGENTS.md) 第三、四章，这里只列改代码时最容易漏的点。

## 分层

`Controller → Service → Manager → Mapper`。Controller 只做参数校验和结果封装，
禁止直接调 Mapper，禁止循环依赖。Service 一律接口 + `impl` 实现类。

业务域开发位置（starter 化后）：狗粮演示业务放 `gentry-start` 的 `com.gentry.start` 下加包；
真实项目业务放消费项目自己仓库；**不要往 starter 里加业务域**，也不要新建 Maven 模块。
最小完整样例：`com.gentry.rbac.dept`（树形 + 数据权限 + 操作日志，在 rbac-starter 内仅供照抄）。

## 必须做

- 实体继承 `TenantEntity`（租户表）或 `BaseEntity`（全局表）；日志表不继承
- 公共字段交给 `AutoFillHandler`，**不要手动 set** create_by / create_time / tenantId
- 取租户用 `UserContext.getTenantId()`，**不硬编码 `tenant_id = 1`**
- 抛异常用 `throw new BizException(ErrorCode.XXX)`，禁止 `RuntimeException`，
  禁止 Controller 里 `return R.fail`
- 参数校验用 `@Valid` + DTO 上的约束注解
- 写接口按 `/api/v1/{module}` 的 REST 形状，返回 `R<T>`
- 权限注解 `@SaCheckPermission("xxx:yyy:zzz")` 的串必须与 `sys_menu.permission` 完全一致，
  加了注解就要配一条 Flyway 迁移插菜单和 `sys_role_menu`
- 登录类和关键写入接口加 `@RateLimit(RateLimitKeyType.IP, ...)`
- 异步任务用 `TraceUtils.wrap(...)` 包住，否则 traceId 丢失
- 新增 Redis Key 登记到 `RedisKeyDefines`，且必须有 TTL

## 测试

Service impl 行覆盖 ≥ 90%、分支 ≥ 80%；AOP 切面每个分支至少一个用例；
Controller 至少一个集成测试。命名 `方法_场景_预期`。

基线：`gentry-core-spring-boot-starter` 133 个测试、`gentry-rbac-spring-boot-starter` 72 个、
全量 `mvn test` 364 个。

**别用 `mvn -pl <module> test` 当验证手段**：单模块构建会从 `~/.m2` 解析
`gentry-core-spring-boot-starter`，拿到上次 `install` 的旧产物 —— 改了 core 的类或
`resources/i18n/*.properties` 后会看到「明明加了却读不到」这类假象。跑全 reactor
的 `mvn test`，或先 `mvn -q -DskipTests install`。
