# 技术宪法 (AGENTS.md)

> **适用范围**：基于本脚手架派生的所有项目 —— 全栈开发的强制约定
> **本文件是唯一权威**。与其他文档冲突时以本文件为准；本文件与代码冲突时，先改文件再改代码。

---

## 零、这个仓库是什么

一个**多租户 RBAC 权限脚手架**。它不是示例工程，是所有新项目的起点：clone 之后
权限体系、布局体系、横切基础设施已经跑通并有测试覆盖，你只需要往里加业务模块。

已经交付的能力：

| 域 | 能力 |
|----|------|
| 数据库 | MySQL（默认）/ PostgreSQL / SQLite 三选一，Spring Profile 切换，同一套业务代码 |
| 多租户 | `tenant_id` 全局自动隔离，三层跳过机制 |
| 认证 | Sa-Token JWT + Redis Session + Token 黑名单（单点登出/强制下线） |
| 授权 | 菜单/按钮权限点 + `@SaCheckPermission`，前端按 permissions 渲染 |
| 数据权限 | 五档范围（全部/本部门及子部门/本部门/仅本人/自定义），`@DataScope` 切面注入 |
| 平台级权限隔离 | `sys_menu.is_platform` 标记平台级权限点，租户管理员既拿不到也分配不了 |
| 系统管理 | 租户、用户、角色、菜单、部门、字典、操作日志、登录日志、在线用户 |
| 横切基础设施 | 全局异常、自动填充、Jackson 统一序列化、请求日志、限流、防重提交、TraceId |
| 运维 | Redis 监控（INFO/Key CRUD/慢日志）、Actuator + Prometheus 指标 |
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
| `gentry-core` | 横切基础设施（异常/多租户/自动填充/追踪/限流/数据权限/JWT 黑名单） | 无 |
| `gentry-business` | 业务逻辑，按业务域分包（现有 `rbac`，新业务平级新增） | core |
| `gentry-monitor` | 运维监控（Redis 监控） | core |
| `gentry-start` | 启动入口 + Flyway 迁移（3 种数据库方言）+ 配置 | 全部 |

新业务域**不要**新建 Maven 模块，在 `gentry-business` 下加包即可；
只有独立部署诉求出现时才拆模块。

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

照抄 `com.gentry.rbac.dept`（最小完整样例：树形 + 数据权限 + 操作日志）。

---

## 四、横切基础设施（gentry-core）

> 架构文档：`doc/design/architecture/全局基础设施架构设计.md`
> 详细设计：`doc/design/modules/core/P0-*.md` ~ `P2-*.md`

| 优先级 | 组件 | 核心类 |
|--------|------|--------|
| P0 | 全局异常处理 | `GlobalExceptionHandler` |
| P0 | 实体基类 | `BaseEntity`, `TenantEntity` |
| P0 | 自动填充 | `AutoFillHandler` |
| P0 | 多租户拦截 | `GentryTenantManager`, `@IgnoreTenant` |
| P0 | 权限接口 | `StpInterfaceImpl`（在 business/rbac/security） |
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
  → TenantManager(追加 tenant_id) → AutoFillHandler(填充公共字段)
  → DataScope(追加部门过滤)
→ GlobalExceptionHandler → Jackson 序列化 → 响应日志
```

### 新模块接入五步

**Step 1 — 实体继承基类**

| 场景 | 继承类 | 得到的字段 |
|------|--------|-----------|
| 租户业务表（`biz_*`、大部分 `sys_*`） | `TenantEntity` | tenant_id + create_by/time + update_by/time + deleted |
| 全局表（`sys_menu`、`sys_tenant`、关联表） | `BaseEntity` | create_by/time + update_by/time + deleted |
| 纯追加日志表（`sys_oper_log`、`sys_login_log`） | 不继承 | 手动维护 tenant_id / create_time，无 deleted |

`AutoFillHandler` 自动填 create_by/create_time/tenantId，**不要手动 set**。

**Step 2 — 查询用 Context + 数据权限**

```java
@DataScope(deptIdField = "dept_id")
public List<Order> list(OrderQueryDTO query) {
    Long tenantId = UserContext.getTenantId();      // 不写死 tenantId
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

### 多租户规则

- 默认单租户形态：所有用户 `tenant_id=1`
- `DEFAULT_TENANT_ID = 1L` 是平台基座，由 `common/V2__init_data.sql` 创建（三种数据库通用），**不可删除**
- 业务租户 id 用雪花算法
- `SUPER_ADMIN(role_id=-1)` 平台级（可见租户管理）；`ADMIN(role_id=1)` 租户级
- **不硬编码 `tenant_id=1`**，一律 `UserContext.getTenantId()`
- 新增业务表必须含 `tenant_id`（全局表除外）
- 三层跳过：`ignoreTables()` 全局表 ｜ `@TenantIgnore` 单 Entity ｜ `@IgnoreTenant` 单方法

### 平台级权限隔离

> 详细设计：`doc/design/modules/rbac/modules/平台级权限隔离/详细设计.md`

**每个租户的 `admin` 都是该租户下的最高权限**，但「租户下的最高权限」不等于「平台权限」。
跨租户的能力（租户管理）和影响共享基础设施的破坏性操作（Redis 删 Key / 清慢日志）
只属平台超管。判据是 `sys_menu.is_platform`（`1`=平台级），两道守卫：

| 守卫 | 位置 | 作用 |
|------|------|------|
| 建租户基线 | `TenantServiceImpl.assignTenantScopedMenusToRole` | 新租户 ADMIN 只拿 `is_platform=0` 的菜单 |
| 分配时校验 | `RoleServiceImpl.assignMenus` | 非平台超管提交含平台级 menuId → `PLATFORM_MENU_FORBIDDEN(40004)` + `log.warn` 审计 |

两道都必须在：租户 ADMIN 握有 `system:role:assignMenu`，只修基线的话它能自己勾回来。

- 新增平台级权限点：写 Flyway 迁移把 `sys_menu.is_platform` 置 1，**顺带
  `DELETE sys_role_menu` 清掉已发出的授权**（参照 `V14__add_menu_is_platform.sql`）
- **`is_platform` 不可由接口设置**：`MenuServiceImpl.create` 固定写 0 且不从 DTO 取，
  `update` 不碰它。租户管理员有 `system:menu:edit`，能改标记就等于能自提权
- 前端 `PermissionPage` 按后端下发的 `isPlatform` 隐藏节点，依据是
  `UserInfoVO.platformAdmin`（**后端算，前端不从 roles 自行推导**）。
  隐藏是体验层，不是安全边界

### 常见踩坑

| 坑 | 正解 |
|---|---|
| `throw new RuntimeException` | `BizException(ErrorCode.XXX)` |
| 手填 `tenant_id = 1` | `UserContext.getTenantId()` |
| Controller 里手写参数校验 | `@Valid` + DTO 约束注解 |
| Mapper 漏 tenant_id | Entity 继承 `TenantEntity`，自动追加 |
| 定时任务里 UserContext 为空 | `@IgnoreTenant` 或手动 set |
| 登录接口无限流 | 必须 `@RateLimit(IP)` |
| 加了权限注解但没插菜单数据 | 写 Flyway 迁移插 `sys_menu` + `sys_role_menu` |
| 新增跨租户/共享资源的权限点，忘了它不该给租户管理员 | 迁移里置 `is_platform=1`，并 `DELETE sys_role_menu` 清已发出的授权 |

### Redis / 缓存

- Key 前缀 `{module}:*`；**严禁无 TTL 的 String Key**
- 新增 Key 请登记到 `RedisKeyDefines`，Redis 监控页会展示
- 高频本地读用 Caffeine，跨节点共享用 Redis

---

## 五、数据模型（RBAC）

```
Tenant → User / Role / Dept        （租户隔离）
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

本脚手架同一套业务代码支持 MySQL（默认）/ PostgreSQL / SQLite，通过 Spring Profile 切换：

```bash
bash scripts/dev_up.sh                 # 默认 db=mysql
bash scripts/dev_up.sh --db=postgresql
bash scripts/dev_up.sh --db=sqlite     # 文件型库，不需要起容器
```

连接配置在 `application-{mysql,postgresql,sqlite}.yml`；Flyway 迁移目录分
`common/`（三库通用，绝大多数迁移放这里）+ `mysql/` `postgresql/` `sqlite/`（各自方言，
目前只有 V1 建表）。详细的兼容写法表、何时需要分方言，见
`.kiro/steering/database-migration.md`。

- 表命名：`{module}_{entity}`（业务）｜ `sys_{entity}`（系统）｜ `{e1}_{e2}_rel`（关联）｜ `{entity}_log`（日志）
- 必填字段：`id`(BIGINT 雪花) ｜ `tenant_id`(非全局表) ｜ `create_time` ｜ `update_time` ｜ `deleted`(SMALLINT)
- 当前时间用 `CURRENT_TIMESTAMP`，**不要用 `NOW()`**（SQLite 不认这个函数名）
- 逻辑删除：`deleted=0/1`；唯一索引在 PostgreSQL/SQLite 用 `WHERE deleted = 0` 局部索引，
  MySQL 不支持局部索引，把 `deleted` 纳入组合唯一键
- 变更**只能追加** Flyway 迁移 `V{n}__xxx.sql`，已发布的迁移文件不可修改
- JSON 类型的列：Java 侧是裸 `String` 就用 `TEXT`，不要用 `JSONB`/`JSON` 原生类型
  （三库语法和函数都不一样，没有跨库收益就不引入方言依赖）

只想固定用一种数据库？删掉不用的两个厂商目录，`application.yml` 的
`spring.profiles.active` 写死，`gentry-start/pom.xml` 删掉不需要的驱动依赖。

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
- **主题**：**改配色只动 `theme/argonColors.ts`**，它是全站配色的唯一源头，两条下游自动跟随：
  - antd 组件 ← `theme/argonTheme.ts` 把色板灌进 ConfigProvider token
  - `styles/argon.less` ← `vite.config.ts` 用 `theme/argonLessVars.ts` 把色板编译成
    `@ps-*` Less 变量注入（写错变量名编译期报错）
  组件里要色值用 `theme.useToken()` 取语义 token，纯文字灰阶直接用
  `<Typography.Text type="secondary">`；**禁止在 .tsx / .less 里写死 hex 或 rgba 调色板色值**
  （`theme/argonLessVars.test.ts` 会拦住 .less 的违规）。
  `/dev/style` 是组件样式对照页，为便于跟 Argon 原版比对，该页允许写死。

---

## 九、测试要求

| 类型 | 要求 |
|------|------|
| Service impl | 行覆盖 ≥ 90%，分支 ≥ 80% |
| Controller | 至少 1 个集成测试 |
| AOP 切面 | 每个分支至少 1 个用例 |
| 前端组件 | Pro 组件与布局组件必须有 Vitest 用例 |
| 前端文案 | **不写中文字面量**，`src/locales/noHardcodedText.test.ts` 会拦（例外要进该文件的 ALLOW 并写明理由） |
| 改动 core | 先跑 `mvn -pl gentry-core test`（基线 130 个测试全绿） |
| 改动 RBAC | 先跑 `mvn -pl gentry-business test`（基线 70 个测试全绿） |

TDD：测试先行 → 红灯 → 最小实现 → 绿灯 → 补覆盖率 → 重构。
测试命名 `方法_场景_预期`。

提交前必须全绿：

```bash
cd backend  && mvn test          # 后端 353 个测试
cd frontend && npm test          # 前端 111 个测试
cd frontend && npx tsc -b        # 类型检查
```

动了页面或权限，还要跑 UI E2E（需前后端都起着）：

```bash
cd frontend && npm run test:e2e  # 90 个 Playwright 用例
```

**`mvn -pl <module> test` 不可信**：单模块构建会从 `~/.m2` 解析 `gentry-core`，
拿到的是上次 `install` 的旧产物。改了 core 的类或 `resources/i18n/*.properties` 之后
必须跑全 reactor 的 `mvn test`，否则会看到「明明加了资源却读不到」这类假象。

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

### 内置账号（common/V2__init_data.sql）

| 账号 | 密码 | 角色 |
|------|------|------|
| chenli | Chenli@2026 | SUPER_ADMIN（平台超管） |
| admin | Abc@123456 | ADMIN（租户管理员） |
| zhangsan | Abc@123456 | ADMIN |

**上生产前必须改掉这三个账号的密码，并外置 `SA_TOKEN_JWT_SECRET_KEY`。**
