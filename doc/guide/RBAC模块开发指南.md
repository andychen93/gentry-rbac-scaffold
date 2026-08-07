# RBAC 模块开发指南

> **版本**：v1.1.0
> **日期**：2026-04-25
> **适用对象**：后端开发、前端开发、AI 编程

---

## 一、项目现状

### 1.1 已完成的工作

| 模块 | 完成内容 | 代码位置 |
|------|---------|---------|
| **全局基础设施** | 10 个组件全部实现并编译通过 | `backend/precision-core/` |
| **RBAC 后端 - 用户管理** | Controller/Service/Mapper/Entity/DTO/VO | `backend/precision-business/src/main/java/com/precision/rbac/user/` |
| **RBAC 后端 - 租户管理** | Controller/Entity/Mapper/VO | `backend/precision-business/src/main/java/com/precision/rbac/tenant/` |
| **RBAC 后端 - 角色/菜单** | Entity/Mapper（基础设施骨架） | `backend/precision-business/src/main/java/com/precision/rbac/role/` + `menu/` |
| **RBAC 后端 - 权限接口** | StpInterfaceImpl（SaSession 缓存） | `backend/precision-business/src/main/java/com/precision/rbac/security/` |
| **前端 - 登录页面** | LoginPage | `frontend/src/pages/login/` |
| **前端 - 用户管理页面** | UserPage + 弹窗组件 | `frontend/src/pages/user/` |

### 1.2 待开发的 RBAC 功能

| 序号 | 子模块 | 后端详细设计 | 前端详细设计 | 后端状态 | 前端状态 |
|------|--------|-------------|-------------|---------|---------|
| 1 | 租户管理 | `modules/租户管理/后端详细设计.md` | `modules/租户管理/前端详细设计.md` | 部分（Entity/Mapper 已有，缺 Service/完整 CRUD） | 未开始 |
| 2 | 租户登录模式 | `modules/租户管理/默认租户与登录模式-详细设计.md` | `modules/租户管理/默认租户与登录模式-前端详细设计.md` | 未开始 | 未开始 |
| 3 | 用户管理 | `modules/用户管理/后端详细设计.md` | `modules/用户管理/前端详细设计.md` | 已有基础框架（需测试和补全） | 已有基础页面（需补全） |
| 4 | 角色管理 | `modules/角色管理/后端详细设计.md` | `modules/角色管理/前端详细设计.md` | 仅 Entity/Mapper | 未开始 |
| 5 | 菜单管理 | `modules/菜单管理/后端详细设计.md` | `modules/菜单管理/前端详细设计.md` | 仅 Entity/Mapper | 未开始 |
| 6 | 部门管理 | `modules/部门管理/后端详细设计.md` | `modules/部门管理/前端详细设计.md` | 未开始 | 未开始 |
| 7 | 字典管理 | `modules/字典管理/后端详细设计.md` | `modules/字典管理/前端详细设计.md` | 未开始 | 未开始 |
| 8 | 日志管理 | `modules/日志管理/后端详细设计.md` | `modules/日志管理/前端详细设计.md` | 未开始 | 未开始 |
| 9 | 公共基础设施 | `doc/design/modules/core/`（P0~P2 共 10 组件） | - | 已实现 | - |
| 10 | 默认租户与登录模式 | `modules/租户管理/默认租户与登录模式-详细设计.md` | `modules/租户管理/默认租户与登录模式-前端详细设计.md` | 未开始 | 未开始 |
| 11 | 登录菜单动态渲染 | - | `modules/用户管理/登录菜单动态渲染-前端详细设计.md` | - | 未开始 |

> **设计文档路径**：`doc/design/modules/rbac/modules/{模块名}/`

---

## 二、开发原则

### 2.1 文档驱动，禁止脱离设计编码

**所有代码必须严格按照详细设计文档实现。**

```
详细设计文档 → 编码实现 → 单元测试 → 代码 Review
     ↑                                        |
     └──── 发现设计问题？反馈修改设计 ←─────────┘
```

| 规则 | 说明 |
|------|------|
| **禁止自行定义任务** | 开发任务来源于详细设计文档，不可凭空添加功能 |
| **禁止脱离设计改接口** | API 路径、参数、返回值必须与设计文档一致 |
| **发现设计问题先改文档** | 如果详细设计有遗漏或错误，先修改设计文档再编码 |
| **YAGNI** | 文档没写的功能不要加 |

### 2.2 技术栈确认

| 项 | 技术选型 | 注意事项 |
|----|---------|---------|
| ORM | **MyBatis-Flex 1.11.6** | 不是 MyBatis-Plus，注解 API 不同 |
| 认证 | **Sa-Token 1.38.0** | StpUtil 校验权限/角色 |
| 实体基类 | **BaseEntity / TenantEntity** | 公共字段自动填充，无需手动赋值 |
| 统一响应 | **R\<T\>** | 所有 API 必须用 R.ok() / R.fail() 包装 |
| 错误码 | **ErrorCode 枚举** | 业务异常用 `throw new BizException(ErrorCode.XXX)` |
| 前端 | **React 18 + Ant Design 5** | 参考已有 LoginPage/UserPage 风格 |

### 2.3 MyBatis-Flex 关键 API

**本项目使用 MyBatis-Flex，不是 MyBatis-Plus！** 常见注解差异：

| 场景 | MyBatis-Plus（错误） | MyBatis-Flex（正确） |
|------|---------------------|---------------------|
| 表名注解 | `@TableName("sys_user")` | `@Table("sys_user")` |
| 主键注解 | `@TableId(type = IdType.AUTO)` | `@Id(keyType = KeyType.Auto)` |
| 列注解 | `@TableField` | `@Column` |
| 逻辑删除 | `@TableLogic` | `@Column(isLogicDelete = true)` |
| 租户字段 | `@TableField(fill = ...)` | `@Column(tenantId = true)` |
| 查询构建 | `LambdaQueryWrapper` | `QueryChain.of(Entity.class).where(...)` |

---

## 三、推荐开发顺序

按依赖关系，建议按以下顺序开发后端（前端可并行）：

```
Phase 1: 租户管理（后端 CRUD 完善 + 前端页面）
    ↓
Phase 2: 部门管理（后端 + 前端）— 其他模块依赖 deptId
    ↓
Phase 3: 菜单管理（后端 + 前端）— 角色管理依赖菜单数据
    ↓
Phase 4: 角色管理（后端 + 前端）— 依赖菜单 + 部门
    ↓
Phase 5: 用户管理完善（后端补全 + 前端完善）— 依赖角色
    ↓
Phase 6: 字典管理（后端 + 前端）— 独立模块
    ↓
Phase 7: 日志管理（后端 + 前端）— 独立模块
    ↓
Phase 8: 默认租户与登录模式（联调）
```

> **注意**：Phase 1（租户管理）的创建租户功能需要调用 DeptService、RoleService、UserService，而这些 Service 分别在 Phase 2、4、5 才开发。建议 Phase 1a 先实现 TenantService 基础 CRUD（不含自动初始化），Phase 8 再补充创建租户自动初始化编排逻辑。

---

## 四、每个模块的开发步骤

### 4.1 后端开发步骤（TDD）

```
1. 阅读后端详细设计文档
2. 按设计文档的 ER 模型创建/确认 Entity（继承 BaseEntity 或 TenantEntity）
3. 编写 Mapper 接口
4. 编写 Service 接口和实现类的单元测试（TDD 红灯）
5. 实现 Service 逻辑
6. 运行测试（TDD 绿灯）
7. 编写 Controller
8. 全量编译验证
9. 提交代码
```

### 4.2 前端开发步骤

```
1. 阅读前端详细设计文档
2. 参考已有 LoginPage/UserPage 的代码风格
3. 按设计文档创建页面组件
4. 对接后端 API（使用 TanStack Query）
5. 联调测试
6. 提交代码
```

### 4.3 提交规范

```bash
git commit -m "feat(rbac): 实现角色管理 CRUD"
git commit -m "feat(rbac): 实现菜单管理树形结构"
git commit -m "fix(rbac): 修复用户分配角色缓存未清除"
git commit -m "test(rbac): 补充角色管理单元测试"
```

---

## 五、关键设计文档索引

### 5.1 架构与概要设计

| 文档 | 路径 | 内容 |
|------|------|------|
| RBAC 概要设计 | `doc/design/modules/rbac/概要设计.md` | 模块整体架构、ER 模型、接口列表 |
| RBAC 架构图 | `doc/design/modules/rbac/RBAC模块架构图.drawio` | 模块关系图 |
| 全局基础设施架构 | `doc/design/architecture/全局基础设施架构设计.md` | 请求流水线、Filter 链、组件依赖 |

### 5.2 全局基础设施详细设计（已实现）

| 组件 | 概要设计 | 详细设计 |
|------|---------|---------|
| 全局异常处理 | `doc/design/modules/core/P0-全局异常处理-概要设计.md` | `P0-全局异常处理-详细设计.md` |
| 自动填充处理器 | `doc/design/modules/core/P0-自动填充处理器-概要设计.md` | `P0-自动填充处理器-详细设计.md` |
| 多租户拦截器 | `doc/design/modules/core/P0-多租户拦截器-概要设计.md` | `P0-多租户拦截器-详细设计.md` |
| StpInterface 权限接口 | `doc/design/modules/core/P0-StpInterface权限接口-概要设计.md` | `P0-StpInterface权限接口-详细设计.md` |
| Jackson 全局配置 | `doc/design/modules/core/P1-Jackson全局配置-概要设计.md` | `P1-Jackson全局配置-详细设计.md` |
| 请求日志过滤器 | `doc/design/modules/core/P1-请求日志过滤器-概要设计.md` | `P1-请求日志过滤器-详细设计.md` |
| 数据权限拦截器 | `doc/design/modules/core/P1-数据权限拦截器-概要设计.md` | `P1-数据权限拦截器-详细设计.md` |
| 全链路追踪 | `doc/design/modules/core/P2-全链路追踪-概要设计.md` | `P2-全链路追踪-详细设计.md` |
| 接口限流 | `doc/design/modules/core/P2-接口限流-概要设计.md` | `P2-接口限流-详细设计.md` |
| 重复提交防护 | `doc/design/modules/core/P2-重复提交防护-概要设计.md` | `P2-重复提交防护-详细设计.md` |

### 5.3 RBAC 子模块详细设计

| 子模块 | 后端详细设计 | 前端详细设计 |
|--------|-------------|-------------|
| 租户管理 | `doc/design/modules/rbac/modules/租户管理/后端详细设计.md` | `modules/租户管理/前端详细设计.md` |
| 默认租户登录 | `modules/租户管理/默认租户与登录模式-详细设计.md` | `modules/租户管理/默认租户与登录模式-前端详细设计.md` |
| 用户管理 | `modules/用户管理/后端详细设计.md` | `modules/用户管理/前端详细设计.md` |
| 角色管理 | `modules/角色管理/后端详细设计.md` | `modules/角色管理/前端详细设计.md` |
| 菜单管理 | `modules/菜单管理/后端详细设计.md` | `modules/菜单管理/前端详细设计.md` |
| 部门管理 | `modules/部门管理/后端详细设计.md` | `modules/部门管理/前端详细设计.md` |
| 字典管理 | `modules/字典管理/后端详细设计.md` | `modules/字典管理/前端详细设计.md` |
| 日志管理 | `modules/日志管理/后端详细设计.md` | `modules/日志管理/前端详细设计.md` |

---

## 六、注意事项

1. **全局基础设施已实现**：异常处理、多租户、自动填充、追踪、限流等已全部就绪，开发业务代码时直接使用即可，不需要重复实现
2. **Entity 继承基类**：所有租户表继承 `TenantEntity`，全局表继承 `BaseEntity`，公共字段无需手动赋值
3. **错误处理**：Service 层抛 `BizException(ErrorCode.XXX)`，GlobalExceptionHandler 自动处理，Controller 不需要 try-catch
4. **租户隔离**：所有 SQL 自动追加 `tenant_id` 条件，全局表通过 `ignoreTables()` 配置，跨租户查询用 `@IgnoreTenant`
5. **权限缓存**：`StpInterfaceImpl` 已实现，角色/权限变更时需调用 `clearUserCache()` 清除缓存
6. **SQL 脚本**：位于 `sql/rbac/` 目录，使用 Flyway 管理，新表需创建新版本迁移脚本

---

## 变更记录

| 版本 | 日期 | 修改人 | 变更内容 |
|------|------|--------|---------|
| v1.0.0 | 2026-04-12 | Claude | 初始版本 |
| v1.1.0 | 2026-04-25 | Claude | 补充遗漏子模块（公共基础设施、默认租户与登录模式、登录菜单动态渲染）；补充 Phase 1 开发顺序注意事项 |
