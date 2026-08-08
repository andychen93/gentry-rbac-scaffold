# API 集成测试设计文档

- 日期：2026-08-08
- 范围：RBAC 脚手架全部 HTTP API（10 个 Controller / 65 个端点）
- 目标：所有 API 都有 HTTP 层集成测试覆盖，`mvn test` 全绿

## 1. 背景与现状

- 现有 `UserManagementIntegrationTest` 名为集成测试，实为**裸 JDBC** 直连 PostgreSQL 验 SQL/业务规则，**未打任何 HTTP、未过 Sa-Token**。因此当前 65 个端点的 HTTP 层覆盖为 **0**。
- 端点分布：Tenant 8、Role 9、Auth 4、User 8、Menu 5、Dept 5、Log 8、Online 2、Dict 9、RedisMonitor 7。
- 测试依赖现状：junit5、mockito、spring-boot-starter-test（含 MockMvc/JsonPath）。无 H2 / Testcontainers / RestAssured。

## 2. 方案选型

选定 **方案 A：全量上下文 HTTP 集成测试**。

| 方案 | 取舍 |
|------|------|
| **A 全量上下文 + 真实 MySQL/Redis（选定）** | `@SpringBootTest`+`MockMvc` 打真实 `/api/v1/...`，完整过 Sa-Token 鉴权/权限/租户/数据权限。用当前已起的 MySQL+Redis，本机即绿。 |
| B `@WebMvcTest` 切片 + mock service | 快、隔离，但鉴权/租户/数据权限/SQL 全被绕过，不符合"过鉴权链路"初衷。 |
| C Testcontainers MySQL+Redis | 自包含、适合 CI；但本机 OrbStack 未开、启动慢、要加依赖。留作 CI 升级。 |

## 3. 架构

### 3.1 模块与启动
- 所有 IT 放 `backend/precision-start/src/test/java/com/precision/start/api/**`。启动模块持有 `@SpringBootApplication`，上下文最全，business+monitor 控制器均在 classpath。
- 注解：`@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc` + `@ActiveProfiles("mysql")` + `@Transactional`。
- MockMvc 在测试线程内派发，`@Transactional` 边界包裹 controller 调用，用例结束自动回滚 DB 写。

### 3.2 鉴权（已验证，关键）
- 抽象基类 `BaseApiIT` 提供 `login(username, password)`：**真实 POST `/api/v1/auth/login`** 取 token。原因：登录流程往 SaSession 写入 `tenantId/userId/deptId/permissionList`，`SaTokenConfig` 拦截器据此重建 `UserContext`。若用裸 `StpUtil.login(userId)` 会丢 tenantId，租户隔离失效（已读源码确认）。
- 权限矩阵复用种子用户：
  - **admin(id=2，全权限)** → happy-path；
  - **chenli(id=1，dev 角色，无 `system:user:*`)** → 403；
  - **不带 Authorization 头** → 401。
- `AuthController` 的 IT 本身直接测 `/auth/login`（含错密码/禁用/错租户）。

### 3.3 数据隔离
- 基类 `@Transactional`：写操作（增/改/删）随用例回滚，种子数据不被污染，读用 Flyway 种子（admin/menus/roles）。
- Redis 会话：`@AfterEach` 调 `POST /auth/logout`（黑名单当前 token + `StpUtil.logout`），不累积。
- caveat：极个别 `REQUIRES_NEW` 事务的写不回滚——遇到改用"建唯一测试数据 + 清理"，在该用例注释标注。

### 3.4 公共端点
`/api/v1/auth/login`、`/api/v1/tenants/options`、`/api/v1/monitor/locations/stream`、`/api/v1/notifications/stream`、`/actuator/**` 已在 SaInterceptor 排除清单（[SaTokenConfig:38-44](../../backend/precision-core/src/main/java/com/precision/core/config/SaTokenConfig.java)），无需 token 即可访问。

## 4. 测试矩阵

每个端点 ≥1 用例；鉴权敏感端点额外覆盖：

| 场景 | 覆盖对象 |
|------|----------|
| happy-path（200，断言关键字段） | 全部 65 端点 |
| 401 未登录 | 每个 IT 类挑 1 个代表端点（不带 header） |
| 403 权限不足 | user/role/menu/dept 的写接口（用 chenli 命中 admin 专属权限） |
| 参数校验 400 | login（空用户名）、create（缺必填）等 |
| 业务异常 | login 错密码/禁用、删除 ADMIN、重复唯一键等 |

预计 **~90–110 个用例**，10 个 IT 类（每个 controller 一个）。

## 5. 文件布局

```
backend/precision-start/src/test/java/com/precision/start/api/
├── BaseApiIT.java              # 抽象基类：MockMvc、login()、authed 辅助、登出清理
├── AuthApiIT.java
├── UserApiIT.java
├── RoleApiIT.java
├── MenuApiIT.java
├── DeptApiIT.java
├── TenantApiIT.java
├── DictApiIT.java
├── LogApiIT.java
├── OnlineApiIT.java
└── RedisMonitorApiIT.java
```

## 6. 依赖变更

`backend/precision-start/pom.xml` 增加（test scope）：
- `spring-boot-starter-test`（提供 `@SpringBootTest`/`MockMvc`/JsonPath）

## 7. 运行方式

```bash
export SPRING_DATASOURCE_PASSWORD='CHENLIchenli321!'   # 本机原生 MySQL
mvn -pl precision-start -am test -Dspring.profiles.active=mysql
```
MySQL(:3306) + Redis(:6379) 已起；Flyway 对已迁移库仅校验不改动。

## 8. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 测试上下文起不来（连库/Redis） | 先做 `DeptApiIT` 模板验证全链路，绿后再复制 |
| `@Transactional` 不回滚某些写 | 逐用例核查；遇 `REQUIRES_NEW` 改显式清理并注释 |
| 依赖种子数据计数导致断言脆弱 | 断言用"≥""包含""存在"，不写死精确总数 |
| Redis 会话累积 | `@AfterEach` logout 清理 |
| 本机 PG 版 IT（旧 `UserManagementIntegrationTest`）连不上 | 默认 `Assumptions.assumeTrue` 跳过，不影响新 IT |

## 9. 验收标准

- 10 个 IT 类就位，65 端点 happy-path 全覆盖，401/403/400/业务异常按矩阵覆盖。
- `mvn -pl precision-start -am test -Dspring.profiles.active=mysql` 在本机全绿。
- `npm test` + `npx tsc -b`（前端）保持绿（本变更不动前端）。
