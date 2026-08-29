# RBAC 权限平台（gentry）

多租户 RBAC 权限平台。平台能力（横切基础设施 + RBAC + 监控）以 Spring Boot starter
jar 分发，新项目**引依赖接入，不 fork**；本仓库是平台的唯一迭代源，内置 `gentry-start`
狗粮工程验证 starter 好不好用。
后端 Spring Boot 3.2 + MyBatis-Flex + Sa-Token(JWT)，支持 MySQL（默认）/ PostgreSQL / SQLite 三种数据库，
前端 React 18 + TypeScript + Vite + Ant Design 5。

两种消费方式：

| 你是… | 怎么用 |
|-------|--------|
| 新项目（推荐） | 引 `gentry-bom` + starter 依赖，主类放自己包，零 `@ComponentScan`/`@MapperScan`，从写业务开始。接入步骤见 `AGENTS.md` 第三章「新项目接入」 |
| 平台开发 / 狗粮 | clone 本仓库改平台代码或演示业务（`gentry-start` 的 `com.gentry.start` 下） |

开箱即用的意思是：跑两条命令，就有一个能登录、有菜单、有权限、有租户隔离、
有操作日志的后台系统，你从加业务模块开始，而不是从搭架子开始。

---

## 1. 五分钟跑起来

前置：JDK 21、Maven 3.9+、Node 18+、Docker（或本机已装对应数据库 + Redis 7）。

默认用 MySQL：

```bash
bash scripts/deps_up.sh     # Docker 起 MySQL(3306) + Redis(6379)，等健康
bash scripts/dev_up.sh      # 编译后端 → 起后端(9090) → 起前端(3030)
```

打开 http://localhost:3030

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `Abc@123456` | ADMIN（租户管理员） |
| `chenli` | `Chenli@2026` | SUPER_ADMIN（平台超管，能看到租户管理） |

其他命令：

```bash
bash scripts/dev_status.sh          # 看前后端 + 依赖状态
bash scripts/dev_down.sh            # 停前后端
bash scripts/deps_down.sh           # 停依赖容器（加 --purge 连数据一起删）
bash scripts/dev_up.sh --uuid       # 不想装 Redis？用 UUID Token 模式（会丢失黑名单能力）
```

数据库结构和初始数据由 Flyway 在后端启动时自动建，不需要手工执行 SQL。

### 切换数据库

```bash
bash scripts/deps_up.sh --db=postgresql   # 起 PostgreSQL 而不是 MySQL
bash scripts/dev_up.sh --db=postgresql    # 后端跟着切到 PostgreSQL profile

bash scripts/dev_up.sh --db=sqlite        # 用 SQLite，不需要 deps_up.sh，
                                           # 数据文件落在 backend/gentry-start/gentry.db
```

三种数据库跑的是同一套业务代码，区别只在 Flyway 迁移的建表方言和 `application-{db}.yml`
的连接配置。想固定用一种、把另外两种删掉，看 `.kiro/steering/database-migration.md`
最后一节。SQLite 只建议用于本地体验 / demo，文件级锁，写并发能力弱于 MySQL/PostgreSQL。

---

## 2. 自带了什么

| 域 | 能力 |
|----|------|
| 多租户 | `tenant_id` 自动注入与隔离，三层跳过机制（全局表 / 单实体 / 单方法） |
| 认证 | Sa-Token JWT + Redis Session + Token 黑名单（登出即失效、强制下线） |
| 授权 | 菜单 + 按钮权限点，后端 `@SaCheckPermission`，前端按 `permissions` 渲染 |
| 数据权限 | 五档范围（全部 / 本部门及子部门 / 本部门 / 仅本人 / 自定义），`@DataScope` 切面 |
| 系统管理 | 租户、用户、角色、菜单、部门、字典、操作日志、登录日志、在线用户（可强制下线） |
| 横切基础设施 | 全局异常、公共字段自动填充、Jackson 统一序列化、请求日志脱敏、接口限流、防重复提交、TraceId 全链路 |
| 运维 | Redis 监控（INFO / Key CRUD / 慢日志）、Actuator + Prometheus 指标端点 |
| 前端 | 双 Layout（业务系统 / 系统管理）、动态菜单路由、Argon 主题、Pro 组件（ProTable / QueryForm / CrudFormModal / RowActions / StatusSwitch…） |

登录后落地在「工作台」（`/home`），顶栏右侧的图标在「业务系统 / 系统管理」两套 Layout 间切换。

---

## 3. 目录结构

```
.
├── AGENTS.md                 # 技术宪法：强制约定，动手前必读
├── CLAUDE.md                 # AI 协作入口，指向 AGENTS.md
├── DOC_INDEX.md              # 文档索引
├── backend/
│   ├── pom.xml               # 父 POM（gentry-parent），聚合下面 5 个模块
│   ├── gentry-core-spring-boot-starter/      # 横切基础设施（jar 分发，自动装配）
│   ├── gentry-rbac-spring-boot-starter/      # RBAC + 通知 + Flyway 平台流迁移（V1–V999）
│   │   └── src/main/resources/db/migration/gentry-rbac/
│   │       ├── common/       # 平台流三库通用迁移
│   │       └── mysql/ …      # 平台流各方言迁移
│   ├── gentry-monitor-spring-boot-starter/   # 运维监控（Redis 监控）
│   ├── gentry-bom/           # 三个 starter 的版本收口，消费项目引它
│   └── gentry-start/         # 狗粮：启动入口 + 全栈 IT + 演示业务包
│       └── src/main/resources/db/migration/
│           ├── common/       # 项目流三库通用迁移（V1000 起，狗粮自用）
│           └── mysql/ …      # 项目流各方言迁移
├── frontend/
│   ├── src/components/       # common（通用）/ layout（双 Layout）/ pro（表格表单）
│   ├── src/config/app.ts     # 应用名 / Logo 缩写（改品牌只动这里 + index.html）
│   ├── src/pages/            # home, login, tenant, user, role, menu, dept, dict, log, monitor, dev
│   ├── src/services/         # axios 封装 + 各模块 API
│   ├── src/stores/           # userStore（认证/菜单/权限）、layoutStore（侧栏/主题）
│   ├── src/theme/            # Argon 配色与 antd token
│   ├── src/utils/menuMapper.ts  # ★ 菜单 component → 页面懒加载映射
│   └── e2e/                  # Playwright 用例
├── doc/                      # 需求 / 设计 / 指南 / 标准 / 测试文档
├── sql/                      # 说明 + 一次性全量初始化快照（权威在 Flyway）
├── scripts/                  # 启停脚本 + 接口级 E2E 脚本
└── docker/docker-compose.yml # 本地 PostgreSQL + Redis
```

---

## 4. 加一个业务模块

先分清业务代码放哪（见 AGENTS.md 第三章）：**狗粮演示业务放 `gentry-start` 的
`com.gentry.start` 下；真实项目业务放消费项目自己仓库；不要往 starter 里加业务域**。
下面以「订单管理」为例，五个动作（示例路径按狗粮写，消费项目同理放自己工程）：

**① 后端代码** —— 照抄 `backend/gentry-rbac-spring-boot-starter/src/main/java/com/gentry/rbac/dept`
（最小完整样例：树形 + 数据权限 + 操作日志），在 `com/gentry/start/order` 下建
`controller / service / service/impl / mapper / entity / dto / vo`。
实体继承 `TenantEntity`，Controller 加 `@SaCheckPermission("biz:order:add")`。

**② 数据库** —— 狗粮新增 `backend/gentry-start/src/main/resources/db/migration/common/V1000__create_order.sql`
（项目流**自 V1000 起**，V1–V999 是平台保留段；放 `common/` 不是三个厂商目录，
建表 + 插菜单是纯 DML/标准 DDL，三库通用）：

```sql
CREATE TABLE IF NOT EXISTS biz_order (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    -- ...业务字段...
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT NOT NULL DEFAULT 0
);

INSERT INTO sys_menu (id, parent_id, name, type, sort, path, component, permission)
VALUES (501, 0, '订单管理', 2, 10, '/order', 'pages/order/OrderPage', 'biz:order');
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time)
SELECT (50000 + id), -1, id, CURRENT_TIMESTAMP FROM sys_menu WHERE id = 501;
```

用 `CURRENT_TIMESTAMP` 不要用 `NOW()`（SQLite 不认）。如果表结构涉及局部唯一索引这类
三库语法有差异的地方，才需要分别写到 `common/` 之外的 `mysql/` `postgresql/` `sqlite/`
三个目录，判断标准见 `.kiro/steering/database-migration.md`。

**③ 前端页面** —— `frontend/src/pages/order/OrderPage.tsx`，用 `ProTable` + `CrudFormModal`
（`frontend/src/pages/dept/` 是最简样例）。

**④ 登记页面** —— `frontend/src/utils/menuMapper.ts` 的 `COMPONENT_MAP` 里加一行：

```ts
'pages/order/OrderPage': () => import('../pages/order/OrderPage'),
```

**漏这一步菜单点不开**，这是最容易踩的坑。

**⑤ 文档 + 测试** —— PRD 与详细设计放 `doc/`（形状见 AGENTS.md 第一章），
Service 单测覆盖 ≥ 90%。

细节看 `doc/guide/RBAC模块开发指南.md`。

---

## 5. 新项目怎么接入（不 fork）

平台是活的、持续迭代的，fork 出去的副本三个月后就合不回平台的 bug 修复。
新项目一律**引依赖**（完整步骤见 `AGENTS.md` 第三章「新项目接入」）：

1. 消费项目 `pom.xml` 引 `gentry-bom`（dependencyManagement）+
   `gentry-core-spring-boot-starter` / `gentry-rbac-spring-boot-starter` /
   `gentry-monitor-spring-boot-starter` 三个依赖（版本由 BOM 收口）
2. 主类放自己的包（如 `com.xxx.Application`），**零注解**：不需要
   `@ComponentScan` / `@MapperScan` / `@EnableScheduling`，starter 自动装配
3. `application.yml` 最小清单：datasource + redis + sa-token（`jwt-secret-key` 外置）+
   `spring.messages.basename: i18n/messages,i18n/error,i18n/validation,i18n/export` +
   `mybatis-flex.mapper-locations: classpath*:mapper/**/*.xml` + Flyway 双流 locations
   （模板直接抄 `backend/gentry-start/src/main/resources/application-mysql.yml`）
4. 项目迁移**自 V1000 起**（V1–V999 是平台保留段，禁止复制/修改平台段迁移）
5. 只想要横切能力、不要 RBAC？`gentry.rbac.enabled=false`；不要 Redis 监控？
   `gentry.monitor.enabled=false`

前端（共享包抽取见 `doc/design/architecture/平台化改造概要设计.md` 第 5 章）。

改**显示名称**只改两处：`frontend/src/config/app.ts` 的 `APP_NAME` / `APP_INITIAL`，
以及 `frontend/index.html` 的 `<title>`。

---

## 6. 上生产前的清单

| 项 | 动作 |
|----|------|
| 内置账号 | 改掉 `chenli` / `admin` / `zhangsan` 的密码，或直接删除测试账号 |
| JWT 密钥 | 环境变量 `SA_TOKEN_JWT_SECRET_KEY` 外置，别用 `application.yml` 里的默认值 |
| 数据库口令 | `application-{mysql,postgresql}.yml` 里的 `spring.datasource.password` 走环境变量外置，别提交明文 |
| Redis | 开启密码认证，`spring.data.redis.password` 外置 |
| Actuator | `/actuator/prometheus` 等端点当前无认证，需限制为内网访问或加认证 |
| CORS / HTTPS | 按部署形态配置反向代理 |
| 日志级别 | `logging.level.com.gentry` 从 `DEBUG` 调到 `INFO` |

`application.yml` 里的数据库口令、JWT 密钥都是本地开发默认值，**直接上生产等于没有认证**。

---

## 7. 验证与测试

不需要任何外部依赖，随时可跑：

```bash
cd backend  && mvn test          # 364 个测试
cd frontend && npm test          # 111 个测试（Vitest）
cd frontend && npx tsc -b        # 类型检查
```

需要前后端都起着（`bash scripts/dev_up.sh`）才能跑：

```bash
cd frontend && npm run test:e2e          # 90 个 Playwright UI 用例
                                         # 首次需 npx playwright install chromium
```

接口级验证脚本，只需要后端起着：

```bash
bash scripts/rate_limit_e2e_test.sh      # 限流 + 防重复提交（要求后端刚启动，Caffeine 为空）
bash scripts/trace_e2e_test.sh           # TraceId 透传 + 请求日志脱敏
bash scripts/redis_monitor_e2e_test.sh   # Redis 监控接口与权限矩阵
bash scripts/jwt_e2e_test.sh             # 登录 / 登出 / 黑名单（会改 chenli 的密码）
bash scripts/jwt_force_logout_test.sh    # 强制下线
```

这几个脚本都会打登录接口，而登录带 IP 限流（10 次/60 秒），**连着跑要间隔一分钟**。
`jwt_e2e_test.sh` 的最后一个场景会修改 `chenli` 的密码，跑完记得在页面上改回来
或重建数据库。

---

## 8. 从哪开始读

1. `AGENTS.md` —— 强制约定，唯一权威（含「新项目接入」）
2. `doc/guide/RBAC模块开发指南.md` —— 业务模块怎么写
3. `doc/guide/Core组件开发指南.md` —— 横切组件怎么用
4. `doc/design/architecture/平台化改造概要设计.md` —— starter 化总体设计
5. `doc/design/modules/rbac/概要设计.md` —— RBAC 整体设计
6. `doc/design/modules/rbac/modules/公共基础设施/双Layout布局-前端详细设计.md` —— 前端布局体系
7. `DOC_INDEX.md` —— 其余文档的地图
