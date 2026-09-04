# 技术宪法 (AGENTS.md)

> **适用范围**：基于本平台派生的所有项目 —— 全栈开发的强制约定。
> 平台能力以 starter jar 分发，新项目**引依赖而非 fork**（见第三章「新项目接入」）
> **本文件是唯一权威**。与其他文档冲突时以本文件为准；本文件与代码冲突时，先改文件再改代码。

---

## 零、这个仓库是什么

一个**单组织 RBAC 权限平台**。横切基础设施、RBAC、监控收敛为 Spring Boot starter
jar 分发；本仓库是平台的**唯一迭代源**，同时用 `gentry-start` 当第一个消费者（狗粮）
验证 starter 好不好用。权限体系、布局体系、横切基础设施已经跑通并有测试覆盖，
消费项目从写自己的业务模块开始，而不是从搭架子开始。

已经交付的能力：

| 域 | 能力 |
|----|------|
| 数据库 | MySQL（默认）/ PostgreSQL / SQLite 三选一，Spring Profile 切换，同一套业务代码 |
| 认证 | Sa-Token JWT + Redis Session + Token 黑名单（单点登出/强制下线） |
| 授权 | 菜单/按钮权限点 + `@SaCheckPermission`，前端按 permissions 渲染 |
| 数据权限 | 五档范围（全部/本部门及子部门/本部门/仅本人/自定义），`@DataScope` 切面注入 |
| 系统管理 | 用户、角色、菜单、部门、字典、操作日志、登录日志、在线用户 |
| 横切基础设施 | 全局异常、自动填充、Jackson 统一序列化、请求日志、限流、防重提交、TraceId |
| 运维 | Redis 监控（INFO/Key CRUD/慢日志）、Actuator + Prometheus 指标（以上能力随 starter jar 分发，自动装配） |
| 国际化 | 中/英双语开箱可用。后端 `Accept-Language` + `sys_user.language` 三级链、错误码与校验消息本地化；前端 react-i18next，菜单与字典由后端下发派生 key、前端翻译（切语言零 API 往返） |
| 前端 | 双 Layout（业务系统 / 系统管理）、动态菜单路由、Argon 主题、Pro 组件（ProTable/QueryForm/CrudFormModal…） |

**动手前先读**：`doc/guide/RBAC模块开发指南.md`（业务模块怎么写）、
`doc/guide/Core组件开发指南.md`（横切组件怎么用）。

---

## 一、开发流程标准

遵循 `doc/standards/CCF微服务开发白皮书_v2.2.md`：

- **文档先行**：概要设计 → 详细设计 → 编码。禁止无文档编码。
- **评审门禁**：概要设计通过才进详细设计，详细设计通过才进编码。
- **代码一致性**：Review 的第一检查点是「代码与详细设计文档一致」。

文档落位（照 RBAC 的目录形状放）：

```
doc/requirements/{module}/PRD-{功能}.md
doc/design/modules/{module}/概要设计.md
doc/design/modules/{module}/modules/{功能}/后端详细设计.md
doc/design/modules/{module}/modules/{功能}/前端详细设计.md
doc/test/{module}/{功能}/{功能}-测试执行报告.md
```

写前端详细设计没手感时，翻 `doc/reference/前端详细设计参考/` 里的样板。

---

## 二、技术选型（不要擅自替换）

**后端**：Spring Boot 3.2.5 / JDK 21 / MyBatis-Flex 1.11.6 /
MySQL 8.0+（默认）｜ PostgreSQL 14+ ｜ SQLite 3.35+（三选一，见第七章）/
Sa-Token 1.38 (JWT + Redis) / Caffeine / Flyway 9.22

**前端**：React 18 / TypeScript 5.6 / Vite 6 / Ant Design 5 / Zustand 4 /
TanStack Query 5 / Less

**测试**：JUnit 5 + Mockito + AssertJ + JaCoCo（后端）｜ Vitest + Testing Library（前端）｜ Playwright（E2E）

---

## 三、系统架构

模块化单体，单机部署，预留微服务拆分。

### 后端模块

| 模块 | 职责 | 依赖 |
|------|------|------|
| `gentry-core-spring-boot-starter` | 横切基础设施（异常/自动填充/追踪/限流/数据权限/JWT 黑名单），jar 分发，自动装配 | 无 |
| `gentry-rbac-spring-boot-starter` | RBAC + 通知业务域 + Flyway 平台流迁移（jar 内 `db/migration/gentry-rbac/`，V1–V999 平台保留段），jar 分发，自动装配 | core-starter |
| `gentry-monitor-spring-boot-starter` | 运维监控（Redis 监控），jar 分发，自动装配 | core-starter |
| `gentry-bom` | 三个 starter 的版本收口（dependencyManagement），消费项目引它对齐版本 | 无 |
| `gentry-start` | 狗粮：启动入口 + 项目流 Flyway 迁移（`db/migration/`，V1000 起）+ 全栈 IT（演示业务包待补） | 三个 starter |
| `gentry-parent` | 父 POM，聚合上述模块并管理第三方版本 | 无 |

装配靠各 starter 的 `META-INF/spring/...AutoConfiguration.imports`，不靠主类包扫描。
业务域位置：**狗粮演示业务放 `gentry-start` 的 `com.gentry.start` 下；真实项目业务放
消费项目自己仓库；不要再往 starter 里加业务域**（starter 只装平台能力，装了业务
所有消费项目被迫继承你的业务）。

### 分层规则

```
Controller → Service → Manager → Mapper
```

- Controller：参数校验 + 结果封装，不写业务
- Service：业务逻辑，接口 + `impl` 实现类
- Manager：复杂编排、第三方封装（简单模块可无）
- **禁止** Controller 直接调 Mapper ｜ **禁止** 循环依赖

### 包结构

```
com.gentry.{domain}.{module}/
├── controller/  service/  service/impl/  mapper/
├── entity/  dto/  vo/  enums/
```

照抄 `com.gentry.rbac.dept`（rbac-starter 里的最小完整样例：树形 + 数据权限 + 操作日志；
狗粮里的新业务域在 `gentry-start` 下按同样形状建包）。

### 新项目接入（引依赖，不 fork）

平台是**唯一迭代源**：平台修 bug、发版，消费项目升版本即可；**不要 fork 本仓库加业务**。
接入步骤：

**① 引 BOM + starter**（消费项目 `pom.xml`）：

```xml
<dependencyManagement>
  <dependency>
    <groupId>com.gentry</groupId>
    <artifactId>gentry-bom</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <type>pom</type>
    <scope>import</scope>
  </dependency>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.gentry</groupId>
    <artifactId>gentry-core-spring-boot-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>com.gentry</groupId>
    <artifactId>gentry-rbac-spring-boot-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>com.gentry</groupId>
    <artifactId>gentry-monitor-spring-boot-starter</artifactId>
  </dependency>
</dependencies>
```

当前阶段先 `mvn install` 平台到本地仓库；第二个消费项目出现时再搭私服。
flyway-core / flyway-mysql 已随 rbac-starter 传递，不用自己声明。

**② 主类放自己的包**（如 `com.xxx.Application`），**零注解**：
不需要 `@ComponentScan("com.gentry")`、不需要 `@MapperScan`、不需要
`@EnableScheduling` —— 全部由 starter 的 `AutoConfiguration.imports` 装配。

**③ 最小 application.yml**（完整模板抄 `gentry-start` 的 `application.yml` + `application-mysql.yml` **两个文件**：主 yml 有 sa-token/mybatis-flex/messages/redis，profile yml 有 datasource 与 flyway locations）：

```yaml
spring:
  datasource:            # 数据库连接（必须）
  data.redis: ...        # Redis（Sa-Token JWT 模式依赖，必须）
  messages:
    basename: i18n/messages,i18n/error,i18n/validation,i18n/export   # 平台 i18n 资源在 jar 内
  flyway:
    locations: classpath:db/migration/gentry-rbac/common,classpath:db/migration/gentry-rbac/{厂商},classpath:db/migration/common,classpath:db/migration/{厂商}   # 双流
mybatis-flex:
  mapper-locations: classpath*:mapper/**/*.xml    # 平台 Mapper XML 在 jar 内
sa-token:
  token-style: jwt-simple                       # JWT + Redis Session 模式，漏了会退回默认 token 风格
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET_KEY}      # 外置，别提交
```

**④ 版本段规则**：项目自己的迁移**从 V1000 起**（平台流占用 V1–V999），
放自己工程的 `db/migration/{common,厂商}/`。**禁止把平台段迁移复制进项目、
禁止修改平台段文件** —— 平台段由 starter jar 提供，升级 jar 即升级平台结构。

**⑤ 开关**（可选）：

| 键 | 默认 | 说明 |
|----|------|------|
| `gentry.rbac.enabled` | true | false 时不装配 RBAC 域（只引 core 做纯横切的项目用） |
| `gentry.monitor.enabled` | true | false 时不装配 Redis 监控 |

---

## 四、横切基础设施（gentry-core-spring-boot-starter）

> 架构文档：`doc/design/architecture/全局基础设施架构设计.md`
> 详细设计：`doc/design/modules/core/P0-*.md` ~ `P2-*.md`

| 优先级 | 组件 | 核心类 |
|--------|------|--------|
| P0 | 全局异常处理 | `GlobalExceptionHandler` |
| P0 | 实体基类 | `BaseEntity` |
| P0 | 自动填充 | `AutoFillHandler` |
| P0 | 权限接口 | `StpInterfaceImpl`（在 rbac-starter 的 `rbac/security`） |
| P1 | Jackson 配置 | `JacksonConfig`（Long→String、日期格式、NON_NULL、Asia/Shanghai） |
| P1 | 请求日志 | `RequestLogFilter`（慢请求告警、敏感字段脱敏） |
| P1 | 数据权限 | `@DataScope`, `DataScopeAspect`, `DataScopeContext` |
| P2 | 全链路追踪 | `TraceIdFilter`, `TraceContext`, `TraceUtils` |
| P2 | 接口限流 | `@RateLimit`, `RateLimitAspect`（Caffeine 滑动窗口） |
| P2 | 防重复提交 | `@RepeatSubmit`, `RepeatSubmitAspect`（MD5 指纹） |
| 扩展 | JWT + 黑名单 | `TokenBlacklistService`, `TokenModeConfig` |

### 请求处理流水线

```
TraceIdFilter → RequestLogFilter → RateLimitAspect → RepeatSubmitAspect
→ Sa-Token 认证/鉴权 → Controller → Service → MyBatis-Flex
  → AutoFillHandler(填充公共字段) → DataScope(追加部门过滤)
→ GlobalExceptionHandler → Jackson 序列化 → 响应日志
```

### 新模块接入五步

**Step 1 — 实体继承基类**

| 场景 | 继承类 | 得到的字段 |
|------|--------|-----------|
| 业务表（`biz_*`、大部分 `sys_*`） | `BaseEntity` | create_by/time + update_by/time + deleted |
| 纯追加日志表（`sys_oper_log`、`sys_login_log`） | 不继承 | 手动维护 create_time，无 deleted |

`AutoFillHandler` 自动填 create_by/create_time，**不要手动 set**。

**Step 2 — 查询用数据权限**

```java
@DataScope(deptIdField = "dept_id")
public List<Order> list(OrderQueryDTO query) {
    DataScopeContext.Condition c = DataScopeContext.get();
    return orderMapper.selectList(query, c);
}
```

**Step 3 — Controller 用注解声明横切关注**

| 注解 | 场景 |
|------|------|
| `@RateLimit(IP, count=5~10, period=60)` | 登录 / 关键写入 |
| `@RateLimit(USER_ID, count=100, period=60)` | 高频查询 |
| `@RepeatSubmit(interval=3)` | 防连点 |
| `@SaCheckPermission("biz:order:add")` | 权限点（必须与 `sys_menu.permission` 一致） |
| `@Log(module="订单", type="INSERT")` | 操作日志 |

**Step 4 — 异常用 ErrorCode + BizException**

禁止 `throw new RuntimeException`；禁止 Controller 里 `return R.fail`。
统一 `throw new BizException(ErrorCode.XXX)`。

ErrorCode 分段：系统 10001-10099 ｜ RBAC 20001-20099 ｜ 认证 30001-30099 ｜
安全 40001-40099 ｜ **50001 起留给业务自行分配**。

**Step 5 — 异步任务包 TraceUtils**

```java
executor.submit(TraceUtils.wrap(() -> doWork()));  // ✅ MDC 传递
executor.submit(() -> doWork());                    // ❌ traceId 丢失
```

### 常见踩坑

| 坑 | 正解 |
|---|---|
| `throw new RuntimeException` | `BizException(ErrorCode.XXX)` |
| Controller 里手写参数校验 | `@Valid` + DTO 约束注解 |
| 登录接口无限流 | 必须 `@RateLimit(IP)` |
| 加了权限注解但没插菜单数据 | 写 Flyway 迁移插 `sys_menu` + `sys_role_menu` |

### Redis / 缓存

- Key 前缀 `{module}:*`；**严禁无 TTL 的 String Key**
- 新增 Key 请登记到 `RedisKeyDefines`，Redis 监控页会展示
- 高频本地读用 Caffeine，跨节点共享用 Redis

---

## 五、数据模型（RBAC）

```
User  ↔ Role                        （多对多，sys_user_role）
Role  ↔ Menu                        （多对多，sys_role_menu，Menu 为全局表）
Role  ↔ Dept                        （自定义数据权限，sys_role_dept）
Dept  → Dept                        （树形自关联，ancestors 冗余路径）
DictType → DictData                 （按 dict_type 字符串关联）
```

数据权限范围：`1`=全部 ｜ `2`=本部门及子部门 ｜ `3`=本部门 ｜ `4`=仅本人 ｜ `5`=自定义

菜单类型：`1`=目录 ｜ `2`=菜单（页面） ｜ `3`=按钮（权限点）

---

## 六、API 规范

| 操作 | 方法 | URL |
|------|------|-----|
| 列表 | GET | `/api/v1/{module}` |
| 树查询 | GET | `/api/v1/{module}/tree` |
| 详情 | GET | `/api/v1/{module}/{id}` |
| 创建 | POST | `/api/v1/{module}` |
| 全量更新 | PUT | `/api/v1/{module}/{id}` |
| 删除 | DELETE | `/api/v1/{module}/{id}` |

响应统一 `R<T>`：`{ code, message, data, timestamp, traceId }`，`code=0` 为成功。
前端 `services/request.ts` 已剥一层 axios response，业务层直接拿 `{ code, data }`。

---

## 七、数据库规范（三库支持）

本平台同一套业务代码支持 MySQL（默认）/ PostgreSQL / SQLite，通过 Spring Profile 切换：

```bash
bash scripts/dev_up.sh                 # 默认 db=mysql
bash scripts/dev_up.sh --db=postgresql
bash scripts/dev_up.sh --db=sqlite     # 文件型库，不需要起容器
```

连接配置在 `application-{mysql,postgresql,sqlite}.yml`；Flyway 迁移分**双流**，
每流内部再按 `common/`（三库通用，绝大多数迁移放这里）+ `mysql/` `postgresql/`
`sqlite/`（各自方言，目前只有 V1 建表）分目录：

| 流 | classpath 位置 | 版本段 | 归属 |
|----|---------------|--------|------|
| 平台流 | `db/migration/gentry-rbac/{common,厂商}` | **V1–V999（平台保留段）** | rbac-starter jar 携带，本仓库在 `backend/gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/` |
| 项目流 | `db/migration/{common,厂商}` | **V1000 起** | 消费项目自己；狗粮对应 `backend/gentry-start/src/main/resources/db/migration/` |

`spring.flyway.locations` 同时列两组（保持 `classpath:` 前缀，不要写成 `classpath*:`，
Flyway 的 ClassPathScanner 走 `ClassLoader.getResources()` 天然跨 jar 枚举），
模板抄 `gentry-start` 的 `application.yml` + `application-{profile}.yml` 两个文件。**项目侧禁止复制、修改平台段迁移**
—— 版本段保留是防撞的唯一手段。详细的兼容写法表、何时需要分方言，见
`.kiro/steering/database-migration.md`。

- 表命名：`{module}_{entity}`（业务）｜ `sys_{entity}`（系统）｜ `{e1}_{e2}_rel`（关联）｜ `{entity}_log`（日志）
- 必填字段：`id`(BIGINT 雪花) ｜ `create_time` ｜ `update_time` ｜ `deleted`(SMALLINT)
- 当前时间用 `CURRENT_TIMESTAMP`，**不要用 `NOW()`**（SQLite 不认这个函数名）
- 逻辑删除：`deleted=0/1`；唯一索引在 PostgreSQL/SQLite 用 `WHERE deleted = 0` 局部索引，
  MySQL 不支持局部索引，把 `deleted` 纳入组合唯一键
- 变更**只能追加** Flyway 迁移 `V{n}__xxx.sql`，已发布的迁移文件不可修改
- JSON 类型的列：Java 侧是裸 `String` 就用 `TEXT`，不要用 `JSONB`/`JSON` 原生类型
  （三库语法和函数都不一样，没有跨库收益就不引入方言依赖）

只想固定用一种数据库？删掉不用的两个厂商目录（**项目流和平台流两侧都指你选定的厂商**，
`spring.flyway.locations` 只列该厂商的两条），`application.yml` 的
`spring.profiles.active` 写死，消费项目删掉不需要的驱动依赖。

---

## 八、前端架构

```
App → Layout(双 Layout) → Pages → Components(通用) / Pro(表格表单) → Services → Stores
```

- **动态菜单路由**：后端返回菜单树 → `utils/menuMapper.ts` 的 `COMPONENT_MAP` 映射到懒加载页面。
  新增页面必须在此登记，否则菜单点不开。
- **双 Layout**：路径前缀 `/system`、`/monitor`、`/monitor-center` 归「系统管理」，
  其余归「业务系统」，顶栏图标切换（见 `AppHeader.tsx`）。
- **状态**：`userStore`（token/userInfo/menus/permissions）、`layoutStore`（侧栏/主题）。
  业务状态优先用 TanStack Query，别什么都塞 Zustand。
- **权限渲染**：`useUserStore().hasPermission('system:user:add')`；页面级用 `AccessDenied`。
- **主题**：**改配色只动 `@gentry/kit` 的 `theme/argonColors.ts`**
  （`frontend/packages/gentry-kit/src/theme/`），它是全站配色的唯一源头，两条下游自动跟随：
  - antd 组件 ← kit 的 `theme/argonTheme.ts` 把色板灌进 ConfigProvider token
  - `styles/argon.less` ← `vite.config.ts` 用 kit 的 `theme/argonLessVars.ts` 把色板编译成
    `@ps-*` Less 变量注入（写错变量名编译期报错）
  组件里要色值用 `theme.useToken()` 取语义 token，纯文字灰阶直接用
  `<Typography.Text type="secondary">`；**禁止在 .tsx / .less 里写死 hex 或 rgba 调色板色值**
  （应用侧 `src/theme/argonLessVars.test.ts` 会拦住 .less 的违规）。
  `/dev/style` 是组件样式对照页，为便于跟 Argon 原版比对，该页允许写死。
- **共享层 `@gentry/kit`**：Pro 组件（ProTable/QueryForm/RowActions/StatusSwitch/
  CrudFormModal/PageSelect/SweetAlert）、theme、`usePagedList`、`types/api` 都在
  `frontend/packages/gentry-kit/`，应用侧统一 `import … from '@gentry/kit'`
  （workspace 包，tsconfig/vite 源码直引）。页面与 `menuMapper` 注册留在应用层。

---

## 九、测试要求

| 类型 | 要求 |
|------|------|
| Service impl | 行覆盖 ≥ 90%，分支 ≥ 80% |
| Controller | 至少 1 个集成测试 |
| AOP 切面 | 每个分支至少 1 个用例 |
| 前端组件 | Pro 组件与布局组件必须有 Vitest 用例 |
| 前端文案 | **不写中文字面量**，`src/locales/noHardcodedText.test.ts` 会拦（例外要进该文件的 ALLOW 并写明理由） |
| 改动 core | 先跑 `mvn -pl gentry-core-spring-boot-starter test`（基线 127 个测试全绿） |
| 改动 RBAC | 先跑 `mvn -pl gentry-rbac-spring-boot-starter test`（基线 75 个测试全绿） |

TDD：测试先行 → 红灯 → 最小实现 → 绿灯 → 补覆盖率 → 重构。
测试命名 `方法_场景_预期`。

提交前必须全绿：

```bash
cd backend  && mvn test          # 后端 345 个测试
cd frontend && npm test          # 前端 112 个测试
cd frontend && npx tsc -b        # 类型检查
```

动了页面或权限，还要跑 UI E2E（需前后端都起着）：

```bash
cd frontend && npm run test:e2e  # 77 个 Playwright 用例
```

**`mvn -pl <module> test` 不可信**：单模块构建会从 `~/.m2` 解析
`gentry-core-spring-boot-starter`，拿到的是上次 `install` 的旧产物。改了 core 的类或
`resources/i18n/*.properties` 之后必须跑全 reactor 的 `mvn test`，否则会看到
「明明加了资源却读不到」这类假象。

E2E 的登录 Token 由 `e2e/global-setup.ts` 一次性预登录后落盘复用 —— 登录接口有
IP 限流（10 次/60 秒），**不要在 spec 里逐个 test 走真实登录**，否则整套用例会被限流打挂。
连续重跑整套用例也会撞限流（每轮 3 次预登录），报 `40001 请求过于频繁`，等 60 秒即可。

浏览器语言由 `playwright.config.ts` 锁成 `zh-CN`（**别删**：Chromium 默认 en-US，
会让所有断言中文文案的用例集体变红）。验英文界面见 `e2e/14-i18n.spec.ts`。

**改了 `sys_menu` 的 `permission` / `path`，或增删了菜单/字典的 Flyway 迁移，
必须跑一次 E2E** —— `I18N-007/008` 负责对账语言包 key 与库里派生的 key，
只有它能发现「permission 改名导致译文静默退化成中文」。

---

## 十、附录

### 错误码分段

| 范围 | 分类 |
|------|------|
| 0 | SUCCESS |
| 10001-10099 | 系统错误 |
| 20001-20099 | RBAC 业务 |
| 30001-30099 | 认证 / 数据 |
| 40001-40099 | 安全控制 |
| 50001+ | 业务自行分配 |

### 内置账号（平台流 common/V2__init_data.sql）

| 账号 | 密码 | 角色 |
|------|------|------|
| chenli | Chenli@2026 | ADMIN（管理员，拥有全部权限） |
| admin | Abc@123456 | ADMIN（管理员，拥有全部权限） |
| zhangsan | Abc@123456 | ADMIN |

**上生产前必须改掉这三个账号的密码，并外置 `SA_TOKEN_JWT_SECRET_KEY`。**
