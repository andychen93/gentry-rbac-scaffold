# StpInterface 权限接口 概要设计

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | StpInterface 权限接口概要设计 |
| 版本 | v1.0.0 |
| 创建日期 | 2026-04-12 |
| 最后更新 | 2026-04-12 |
| 负责人 | 技术经理（AI辅助） |
| 所属模块 | gentry-business |
| 优先级 | P0（核心） |

---

# 一、设计目标（What & Why）

## 是什么（What）

StpInterface 权限接口是 Sa-Token 框架的权限数据供给层。通过实现 Sa-Token 的 `StpInterface` 接口，向框架提供当前登录用户的权限列表和角色列表，使得 `@SaCheckPermission`（权限校验注解）和 `@SaCheckRole`（角色校验注解）能够正常工作。

核心职责：

1. **权限列表供给**：根据登录用户ID，查询其拥有的所有权限标识（如 `vehicle:list`、`vehicle:create`）
2. **角色列表供给**：根据登录用户ID，查询其拥有的所有角色编码（如 `admin`、`operator`）
3. **数据查询**：通过 User → UserRole → Role → RoleMenu → Menu 的关联链路查询权限
4. **缓存管理**：利用 Sa-Token Session 缓存权限和角色数据，避免每次请求都查询数据库
5. **缓存失效**：当用户的角色或权限发生变更时，及时清除缓存

## 为什么（Why）

1. **框架集成要求**：Sa-Token 的注解式鉴权（`@SaCheckPermission`、`@SaCheckRole`）依赖 `StpInterface` 实现类来获取用户的权限和角色数据。没有此实现，所有权限注解都无法生效
2. **统一权限模型**：平台采用 RBAC（基于角色的访问控制）模型，权限数据来源于 User → Role → Menu 的关联关系，需要集中查询和转换
3. **性能优化**：权限数据在单次会话中基本不变，利用 Sa-Token Session 缓存可避免每次请求都进行数据库查询
4. **开发便捷**：后端开发只需在接口方法上加注解即可完成权限控制，无需在业务代码中硬编码权限判断逻辑
5. **多租户隔离**：权限查询天然按租户隔离（用户归属租户，角色归属租户，菜单是系统级公共数据）

# 二、定位与上下文（Where & When）

## 在哪里（Where）

位于 `gentry-business` 模块的 `com.gentry.rbac.security` 包中，作为 Sa-Token 框架与业务数据库之间的桥梁。

在权限校验链路中的位置：

```
HTTP Request
  → Sa-Token 拦截器（SaInterceptor）
    → 检测到 @SaCheckPermission("vehicle:list")
      → 调用 StpInterface.getPermissionList(loginId, loginType)
        → 查询数据库 / Session 缓存
        → 返回权限列表 ["vehicle:list", "vehicle:create", ...]
      → 判断列表中是否包含 "vehicle:list"
        → 包含：放行
        → 不包含：抛出 NotPermissionException
```

依赖关系：

- **被依赖方**：所有使用 `@SaCheckPermission` 或 `@SaCheckRole` 注解的 Controller 方法
- **依赖方**：
  - Sa-Token 框架（`cn.dev33.satoken.stp.StpInterface` 接口）
  - 用户相关 Mapper（查询用户-角色-菜单关联关系）
  - Sa-Token Session（缓存权限和角色数据）
  - `UserContext`（获取当前用户信息）

## 在何时（When）

触发场景：

| 触发场景 | 调用方法 | 频率 | 说明 |
|----------|----------|------|------|
| 访问带 `@SaCheckPermission` 的接口 | `getPermissionList()` | 每次请求 | 首次查询后走 Session 缓存 |
| 访问带 `@SaCheckRole` 的接口 | `getRoleList()` | 每次请求 | 首次查询后走 Session 缓存 |
| 角色权限变更 | 手动清除缓存 | 低频 | 管理员修改角色菜单后触发 |
| 用户角色变更 | 手动清除缓存 | 低频 | 管理员修改用户角色后触发 |

# 三、主要用户与交互方（Who）

## 面向谁（Who）

| 角色 | 关注点 |
|------|--------|
| **后端开发** | 在 Controller 方法上加 `@SaCheckPermission` 或 `@SaCheckRole` 注解即可，无需关心权限数据来源 |
| **系统管理员** | 通过角色管理、菜单管理配置权限，变更后即时生效 |
| **安全审计** | 验证权限校验链路完整，无越权漏洞 |

## 与其他组件的交互

| 交互组件 | 交互方式 | 说明 |
|----------|----------|------|
| Sa-Token 框架 | 实现 `StpInterface` | 框架自动调用获取权限/角色列表 |
| `QueryChain`（MyBatis-Flex） | 查询 | 通过 QueryChain 查询 UserRole / Role / RoleMenu / Menu |
| Sa-Token Session | 缓存 | 将权限和角色列表缓存到用户 Session 中 |
| `UserContext` | 读取 | 获取用户ID用于查询 |

# 四、实现思路（How）

## 核心技术方案

### 4.1 StpInterfaceImpl 核心实现

实现 Sa-Token 的 `StpInterface` 接口，提供两个核心方法：

```java
@Component
public class StpInterfaceImpl implements StpInterface {
```

**getPermissionList(loginId, loginType)**

查询链路：

```
loginId (用户ID)
  → 查询 user_role_rel 表，获取角色ID列表
  → 查询 role_menu_rel 表，获取菜单ID列表
  → 查询 sys_menu 表，获取 permission 字段列表
  → 过滤掉空值和无效菜单
  → 返回 Set<String> 权限标识列表
```

SQL 等价逻辑：

```sql
SELECT DISTINCT m.permission
FROM user_role_rel ur
JOIN role_menu_rel rm ON ur.role_id = rm.role_id
JOIN sys_menu m ON rm.menu_id = m.id
WHERE ur.user_id = #{userId}
  AND m.permission IS NOT NULL
  AND m.permission != ''
  AND m.status = 1
  AND m.deleted = 0
```

**getRoleList(loginId, loginType)**

查询链路：

```
loginId (用户ID)
  → 查询 user_role_rel 表，获取角色ID列表
  → 查询 sys_role 表，获取角色编码列表
  → 过滤掉禁用角色
  → 返回 Set<String> 角色编码列表
```

SQL 等价逻辑：

```sql
SELECT DISTINCT r.code
FROM user_role_rel ur
JOIN sys_role r ON ur.role_id = r.id
WHERE ur.user_id = #{userId}
  AND r.status = 1
  AND r.deleted = 0
```

### 4.2 缓存策略

利用 Sa-Token 自带的 Session 机制缓存权限和角色数据：

| 缓存项 | Key | Value | 生命周期 |
|--------|-----|-------|----------|
| 权限列表 | `"permissionList"` | `Set<String>` | 与 Sa-Token Session 一致 |
| 角色列表 | `"roleList"` | `Set<String>` | 与 Sa-Token Session 一致 |

缓存读取逻辑：

```
getPermissionList():
  1. 从 Sa-Token Session 获取缓存
  2. 缓存存在 → 直接返回
  3. 缓存不存在 → 查询数据库 → 写入 Session → 返回
```

### 4.3 缓存失效机制

当权限数据发生变更时，需要主动清除缓存：

| 变更场景 | 失效操作 | 触发方式 |
|----------|----------|----------|
| 修改角色菜单（角色权限变更） | 清除该角色下所有用户的缓存 | `RoleMenuService.updateRoleMenu()` 后调用 |
| 修改用户角色 | 清除该用户的缓存 | `UserRoleService.updateUserRoles()` 后调用 |
| 禁用/启用角色 | 清除该角色下所有用户的缓存 | `RoleService.updateStatus()` 后调用 |
| 禁用/启用菜单 | 清除所有拥有该菜单的用户的缓存 | `MenuService.updateStatus()` 后调用 |
| 删除用户 | 用户登出，缓存自然失效 | `UserService.delete()` 后调用 |

缓存清除方法：

```java
/**
 * 清除指定用户的权限缓存
 */
public static void clearPermissionCache(long userId) {
    StpUtil.getSessionByLoginId(userId, false)
        .ifPresent(session -> session.delete("permissionList"));
}

/**
 * 清除指定用户的角色缓存
 */
public static void clearRoleCache(long userId) {
    StpUtil.getSessionByLoginId(userId, false)
        .ifPresent(session -> session.delete("roleList"));
}
```

### 4.4 特殊角色处理

| 角色类型 | 权限处理 | 说明 |
|----------|----------|------|
| 超级管理员（code = `super_admin`） | 返回 `["*"]` | 拥有所有权限，`@SaCheckPermission` 自动放行 |
| 平台管理员（code = `platform_admin`） | 正常查询 | 按配置的菜单权限 |
| 租户管理员（code = `tenant_admin`） | 正常查询 | 按配置的菜单权限 |
| 普通角色 | 正常查询 | 按配置的菜单权限 |

### 4.5 多租户隔离

权限查询天然按租户隔离：

- `user_role_rel` 表：通过用户关联到租户内的角色
- `sys_role` 表：包含 `tenant_id` 字段，租户间角色独立
- `sys_menu` 表：系统级公共数据，所有租户共享
- `role_menu_rel` 表：通过角色关联到租户内的菜单配置

查询时的租户隔离由多租户拦截器自动处理。

## 关键类和接口设计

| 类名 | 包路径 | 职责 |
|------|--------|------|
| `StpInterfaceImpl` | `com.gentry.rbac.security` | 实现 Sa-Token StpInterface 接口 |
| `PermissionCacheManager` | `com.gentry.rbac.security` | 权限缓存管理（清除缓存） |
| `UserRoleRelMapper` | `com.gentry.business.mapper` | 用户角色关联查询 |
| `RoleMapper` | `com.gentry.business.mapper` | 角色查询 |
| `MenuMapper` | `com.gentry.business.mapper` | 菜单权限查询 |
| `RoleMenuRelMapper` | `com.gentry.business.mapper` | 角色菜单关联查询 |

## 与其他组件的协作关系

```
┌─────────────────────────────────────────────────────────────┐
│                   Sa-Token 权限校验链路                       │
│                                                              │
│  HTTP Request                                                │
│     │                                                        │
│     ▼                                                        │
│  ┌──────────────────────────────────────────┐               │
│  │ SaInterceptor                             │               │
│  │ 检测到 @SaCheckPermission("vehicle:list") │               │
│  └───────────────────┬──────────────────────┘               │
│                      │                                      │
│                      ▼                                      │
│  ┌──────────────────────────────────────────┐               │
│  │ StpInterfaceImpl                          │               │
│  │                                           │               │
│  │ getPermissionList(loginId, loginType)     │               │
│  │   │                                       │               │
│  │   ├→ Session 缓存命中？                    │               │
│  │   │   是 → 返回缓存的权限列表              │               │
│  │   │   否 → 查询数据库 ↓                   │               │
│  │   │                                       │               │
│  │   └→ User → UserRole → Role → RoleMenu   │               │
│  │      → Menu.permission                    │               │
│  │      → 写入 Session 缓存                  │               │
│  │      → 返回 Set<String>                   │               │
│  └───────────────────┬──────────────────────┘               │
│                      │                                      │
│                      ▼                                      │
│          权限列表包含 "vehicle:list"？                         │
│           是 → 放行，继续执行 Controller                     │
│           否 → 抛出 NotPermissionException                  │
│                  → GlobalExceptionHandler 捕获               │
│                  → 返回 R.fail(30003, "权限不足")              │
└─────────────────────────────────────────────────────────────┘
```

# 五、预期效果与验收标准

## 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首次查询耗时 | < 50ms | 涉及多表关联查询 |
| 缓存命中耗时 | < 1ms | 从 Sa-Token Session 读取 |
| 缓存命中率 | > 99% | 单次会话内权限不变 |
| 缓存失效延迟 | < 1s | 权限变更后下次请求即生效 |

## 功能验收标准

| 编号 | 验收项 | 验证方式 |
|------|--------|----------|
| P-001 | 正确返回用户权限列表 | 登录后访问接口，验证 Sa-Token 拦截器能获取权限 |
| P-002 | 正确返回用户角色列表 | 使用 @SaCheckRole 注解，验证角色校验通过 |
| P-003 | @SaCheckPermission 校验通过 | 有权限的用户访问受保护接口正常返回 |
| P-004 | @SaCheckPermission 校验拒绝 | 无权限的用户访问返回 30003 错误 |
| P-005 | @SaCheckRole 校验通过 | 有角色的用户访问受保护接口正常返回 |
| P-006 | @SaCheckRole 校验拒绝 | 无所需角色的用户访问返回 30003 错误 |
| P-007 | 超级管理员拥有全部权限 | super_admin 角色用户访问任意接口均放行 |
| P-008 | 缓存生效 | 连续多次访问同一接口，数据库查询仅执行一次 |
| P-009 | 角色权限变更后缓存失效 | 修改角色菜单后，该角色用户下次请求获取新权限 |
| P-010 | 用户角色变更后缓存失效 | 修改用户角色后，该用户下次请求获取新角色 |
| P-011 | 多租户权限隔离 | 租户A的用户看不到租户B的角色和权限 |
| P-012 | 无角色用户返回空列表 | 未分配角色的用户，权限和角色列表为空集合 |

# 六、配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `precision.permission.cache-enabled` | `true` | 是否启用 Session 缓存 |
| `precision.permission.super-admin-role` | `super_admin` | 超级管理员角色编码 |
| `precision.permission.super-admin-permission` | `*` | 超级管理员权限通配符 |

# 七、参考文档

| 文档 | 说明 |
|------|------|
| Sa-Token 1.38 官方文档 - StpInterface | 权限数据接口定义 |
| Sa-Token 1.38 官方文档 - 注解式鉴权 | @SaCheckPermission、@SaCheckRole 使用 |
| CLAUDE.md 第四章 4.1 | 多租户 + RBAC 权限模型 |
| CLAUDE.md 第四章 4.1 | 数据权限范围说明 |
| CCF 微服务开发白皮书 v2.1 附录 B.1 | 概要设计模板 |
