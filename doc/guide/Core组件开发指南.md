# Core 组件开发指南

> **版本**：v1.0.0
> **更新日期**：2026-05-05
> **面向**：所有在 `gentry-business` 及后续业务模块（vehicle / device / alarm / playback / report）上开发的工程师与 AI。
> **阅读前提**：已熟悉 `CLAUDE.md` §4 全局基础设施章节。

---

## 0. 为什么有这份文档

`gentry-core` 承载了 10 个 P0/P1/P2 级别的横切关注（异常处理、多租户、自动填充、认证、数据权限、链路追踪、限流、防重复、请求日志、Jackson）。它们的作用就是让业务开发者"**专注写业务**"。

但实际落地里最常见的反模式是：

- 自己 `try/catch` 吞异常
- Controller 里 `if (xxx) return R.fail("xxx")`
- Service 里 `new Date()` 或硬编码 `tenant_id = 1`
- 异步任务里日志没 traceId
- 登录接口没加限流，被暴破
- Mapper 写死 SQL 忽略数据权限

本文档把 Core 组件的正确姿势按"场景→组件→示例→注意事项"组织，**不是 API 手册**，而是**配方手册**（cookbook）。

---

## 1. 五步接入清单

任何一个新的业务模块（比如「车辆管理」），按以下顺序铺设代码框架：

```
1. Entity 继承 BaseEntity / TenantEntity        → 自动填充 + 多租户
2. Mapper 继承 BaseMapper + QueryChain          → 多租户 SQL 自动追加
3. Service 方法加 @DataScope                    → 数据权限自动追加
4. Controller 写操作加 @Log + @RepeatSubmit      → 操作日志 + 防连点
                     权限点加 @SaCheckPermission  → 鉴权
                     外部入口加 @RateLimit        → 限流
5. 所有异常 throw BizException(ErrorCode.XXX)    → 统一响应
```

一份最小化但合规的 Vehicle 模块 Controller 示例：

```java
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    public VehicleController(VehicleService s) { this.vehicleService = s; }

    /** V-001 车辆列表 */
    @GetMapping
    @SaCheckPermission("biz:vehicle:list")
    @RateLimit(keyType = RateLimitKeyType.USER_ID, count = 120, period = 60)
    public R<PageResult<VehicleListVO>> list(@Valid VehicleQueryDTO query) {
        return R.ok(vehicleService.list(query));
    }

    /** V-002 新增车辆 */
    @PostMapping
    @SaCheckPermission("biz:vehicle:add")
    @RepeatSubmit(interval = 3)
    @Log(module = "车辆管理", type = "INSERT", title = "新增车辆")
    public R<VehicleDetailVO> create(@Valid @RequestBody VehicleCreateDTO dto) {
        return R.ok(vehicleService.create(dto));
    }
    // ... update / remove 同理
}
```

---

## 2. 组件速查表

### 2.1 横切关注点 → 用什么

| 你在做什么 | 选哪个组件 | 在哪儿标注 |
|---|---|---|
| 统一返回 {code, message, data, traceId} | `R.ok(...)` / `R.fail(...)` | Controller |
| 抛业务错误 | `throw new BizException(ErrorCode.XX)` | 任意层 |
| 参数校验 | `@Valid` + `@NotBlank @Size @Pattern` | DTO |
| 认证检查 | Sa-Token 全局拦截器自动做 | - |
| 权限校验 | `@SaCheckPermission("biz:vehicle:add")` | Controller 方法 |
| 多租户自动隔离 | Entity 继承 `TenantEntity` | Entity |
| 自动填充 create_by / create_time | `AutoFillHandler`（已全局注册） | - |
| 行级数据权限（按部门过滤） | `@DataScope` + Mapper XML 读 `DataScopeContext` | Service 方法 |
| 跳过租户过滤（管理员特殊查询） | `@IgnoreTenant("原因")` | Service 方法 |
| traceId 链路追踪 | 全局自动，MDC 里 `%X{traceId}` 已配置 | - |
| 异步任务保持 traceId | `TraceUtils.wrap(runnable)` | 提交任务处 |
| 接口限流（防刷、防暴破） | `@RateLimit(keyType, count, period)` | Controller 方法 |
| 防止重复提交（连点保存按钮） | `@RepeatSubmit(interval=N)` | Controller POST/PUT |
| 请求日志（方法/路径/耗时） | `RequestLogFilter` 全局自动 | - |
| 敏感字段日志脱敏 | 修改 `RequestLogFilter.SENSITIVE_PATHS` | 新增敏感路径 |
| Long 防 JS 精度丢失 | `JacksonConfig` 全局生效 | - |
| 时间字段格式化 | `JacksonConfig` 统一 yyyy-MM-dd HH:mm:ss | - |
| Token 黑名单 | `TokenBlacklistService`（改密/强制下线/登出场景） | Service 层 |
| 国际化消息 | `I18nUtil.getMessage(code, args)` | 任意层 |

### 2.2 ErrorCode 分段

新增错误码时遵守既有分段：

| 分段 | 模块 |
|---|---|
| 10001-10099 | 系统错误（已占满前 5 个） |
| 20001-20099 | RBAC 业务（用户/角色/菜单/租户/部门） |
| 30001-30099 | 认证 / 数据（Token、权限、数据存在性） |
| 40001-40099 | 安全控制（限流、防重复） |
| 50001-50099 | 设备 |
| 51001-51099 | 车辆（规划） |
| 52001-52099 | 报警（规划） |
| 53001-53099 | 位置（规划） |
| 54001-54099 | 报表（规划） |

---

## 3. 按场景写代码

### 3.1 场景 A：一个普通的写操作（POST/PUT/DELETE）

**配方：** `@SaCheckPermission` + `@RepeatSubmit` + `@Log` + DTO `@Valid` + Service 抛 `BizException`

```java
@PostMapping
@SaCheckPermission("biz:vehicle:add")
@RepeatSubmit(interval = 3)
@Log(module = "车辆管理", type = "INSERT", title = "新增车辆")
public R<VehicleDetailVO> create(@Valid @RequestBody VehicleCreateDTO dto) {
    return R.ok(vehicleService.create(dto));
}
```

Service 里：

```java
@Service
public class VehicleServiceImpl implements VehicleService {

    @Transactional
    public VehicleDetailVO create(VehicleCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();
        // ✅ 业务校验，遇到问题直接抛
        if (vehicleMapper.countByPlate(tenantId, dto.getPlateNumber()) > 0) {
            throw new BizException(ErrorCode.DATA_EXISTS, "车牌号已存在");
        }
        Vehicle v = new Vehicle();
        BeanUtils.copyProperties(dto, v);
        v.setId(IdGenerator.nextId());
        // tenantId / createBy / createTime 不用手工 set，AutoFillHandler 会填
        vehicleMapper.insert(v);
        return toDetailVO(v);
    }
}
```

### 3.2 场景 B：列表查询（带数据权限）

**配方：** `@DataScope` + Mapper XML 分支拼 SQL

```java
@Service
public class VehicleServiceImpl implements VehicleService {

    @DataScope(deptIdField = "dept_id")
    public PageResult<VehicleListVO> list(VehicleQueryDTO query) {
        // DataScopeContext 由切面在方法执行前写入
        DataScopeContext.Condition scope = DataScopeContext.get();
        Long total = vehicleMapper.count(query, scope);
        List<Vehicle> list = total == 0
                ? List.of()
                : vehicleMapper.selectList(query, scope);
        return new PageResult<>(toVOs(list), total, query.getPageNum(), query.getPageSize());
    }
}
```

Mapper XML：

```xml
<select id="selectList" resultType="Vehicle">
    SELECT * FROM biz_vehicle WHERE deleted = 0
    <if test="query.plateNumber != null">
        AND plate_number LIKE CONCAT('%', #{query.plateNumber}, '%')
    </if>
    <include refid="dataScopeFragment"/>
    ORDER BY create_time DESC
    LIMIT #{query.pageSize} OFFSET ${ (query.pageNum - 1) * query.pageSize }
</select>

<sql id="dataScopeFragment">
    <if test="scope != null and !scope.allData">
        <if test="scope.hasDeptFilter">
            AND ${scope.deptIdField} IN
            <foreach collection="scope.deptIds" item="id" open="(" separator="," close=")">#{id}</foreach>
        </if>
        <if test="scope.hasCreatorFilter">
            AND ${scope.createByField} = #{scope.creatorUserId}
        </if>
    </if>
</sql>
```

注意：`GentryTenantManager` 会自动追加 `tenant_id = ?`，不要手动写。

### 3.3 场景 C：登录 / 认证

**配方：** `@RateLimit(IP)` 防暴破 + 用户检验失败抛 `LOGIN_FAILED`

```java
@PostMapping("/login")
@RateLimit(keyType = RateLimitKeyType.IP, count = 10, period = 60,
           message = "登录尝试过于频繁，请稍后重试")
public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest req) {
    return R.ok(authService.login(dto, IpUtil.getClientIp(req), req.getHeader("User-Agent")));
}
```

### 3.4 场景 D：密码 / 账户变更（安全敏感）

**配方：** `@RepeatSubmit` 防连点 + 改密后黑名单旧 Token

```java
@PutMapping("/password")
@RepeatSubmit(interval = 5, message = "密码修改请求过快")
public R<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateDTO dto) {
    userService.updatePassword(dto);
    return R.ok();
}
```

Service 里完成修改后：

```java
TokenBlacklistService.blacklistAllTokensOfUser(userId);  // 吊销该用户所有 Token
StpUtil.kickout(userId);                                   // 踢出 Session
```

### 3.5 场景 E：管理员强制下线 / 删除用户

```java
TokenBlacklistService.blacklistAllTokensOfUser(targetUserId);
StpUtil.kickoutByTokenValue(sessionId);   // 或 StpUtil.kickout(userId)
```

### 3.6 场景 F：异步任务（@Async / Executor.submit）

```java
@Autowired private Executor executor;

public void onLocationReport(LocationEvent evt) {
    // ❌ 错：子线程 MDC 为空，日志无 traceId
    // executor.submit(() -> process(evt));

    // ✅ 对
    executor.submit(TraceUtils.wrap(() -> process(evt)));
}
```

Spring `@Async` 场景同理：配置自定义 `TaskDecorator`，调用 `TraceUtils.wrap`（未来会补充 AsyncConfig 模板）。

### 3.7 场景 G：定时任务（@Scheduled）

`UserContext` 在定时任务线程里为空，不能直接用。有两种写法：

**写法 1 — 用 @IgnoreTenant 跳过多租户：**
```java
@Scheduled(cron = "0 0 2 * * *")
@IgnoreTenant("归档任务跨所有租户")
public void archiveOldData() {
    vehicleMapper.deleteOlderThan(LocalDate.now().minusDays(90));
}
```

**写法 2 — 手动设置 UserContext：**
```java
@Scheduled(fixedDelay = 60_000)
public void syncEachTenant() {
    for (Long tenantId : tenantService.listAllIds()) {
        UserContext.setTenantId(tenantId);
        try {
            doSyncForTenant(tenantId);
        } finally {
            UserContext.clear();
        }
    }
}
```

---

## 4. 常见坑与反模式

| 反模式 | 症状 | 修复 |
|---|---|---|
| `throw new RuntimeException("xxx")` | 500 错误 / 无业务码 | `throw new BizException(ErrorCode.XX)` |
| Controller 写 `return R.fail("xxx")` | 绕过 GlobalExceptionHandler | 抛 BizException |
| 硬编码 `tenant_id = 1L` | 多租户失效 | `UserContext.getTenantId()` |
| 异步任务 `executor.submit(task)` | 日志丢 traceId | `TraceUtils.wrap(task)` |
| 列表接口没加 `@RateLimit` | 可能被扫描打崩 | 至少加 `@RateLimit(IP, 120, 60)` |
| 登录接口没加 `@RateLimit(IP)` | 被暴力破解 | 必须加，且 count ≤ 10 |
| 改密码后原 JWT 仍可用 | 安全漏洞 | 改密码后调 `TokenBlacklistService.blacklistAllTokensOfUser(userId)` |
| 实体不继承基类，自己写 5 个字段 | AutoFillHandler 不生效 | 继承 `TenantEntity` / `BaseEntity` |
| Service 方法里 `new SimpleDateFormat(...)` | 时区/格式不统一 | `JacksonConfig.dateTimeFormat()` 或直接用 LocalDateTime |
| 敏感字段日志打印出来 | 密码泄露到日志 | 新敏感路径加入 `RequestLogFilter.SENSITIVE_PATHS` |
| Mapper 里忘带 `deleted = 0` | 查到已删数据 | 保证所有查询都加 `AND deleted = 0` |
| 新业务错误码占用已有号段 | 冲突 | 查本文档 §2.2 分段，落在对应区间 |

---

## 5. 加新敏感字段脱敏

编辑 `RequestLogFilter.SENSITIVE_PATHS`：

```java
private static final Map<String, List<String>> SENSITIVE_PATHS = Map.of(
    "/api/v1/auth/login", List.of("password"),
    "/api/v1/auth/password", List.of("oldPassword", "newPassword"),
    "/api/v1/users/password", List.of("oldPassword", "newPassword"),
    "/api/v1/bank-cards", List.of("cardNumber", "cvv"),   // ← 新增
    "/api/v1/drivers/*/idcard", List.of("idCardNo")        // ← 支持 Ant 风格
);
```

---

## 6. 加新 Redis 监控 Key 模板

所有用 Redis 的组件要把自己的 Key 模板登记到 `RedisKeyDefines`（在 `gentry-monitor`），便于运维在 `/monitor-center/redis` 页面看清楚：

```java
// backend/gentry-monitor/.../RedisKeyDefines.java
private static final List<RedisKeyDefineVO> DEFINES = List.of(
    new RedisKeyDefineVO("token_mapping",  "Authorization:login:token:*",   "...", 1800L),
    new RedisKeyDefineVO("user_session",   "Authorization:login:session:*", "...", 1800L),
    new RedisKeyDefineVO("jwt_blacklist",  "blacklist:*",                   "...", -2L),
    new RedisKeyDefineVO("token_timeout",  "Authorization:timeout:*",       "...", 1800L),
    // 新增：
    new RedisKeyDefineVO("device_online",  "device:online:*",               "设备在线状态", 300L)
);
```

---

## 7. Core 模块修改守则

- 修改 `gentry-core` 任意类都视为"影响全平台"，提 PR 时必须：
  1. 补充单元测试
  2. 跑通 `mvn -pl gentry-core test`（54 个基线测试不能退）
  3. 跑通 `mvn -pl gentry-business test`（业务 48 个集成测试不能退）
  4. 更新对应 `doc/design/modules/core/P*-*.md` 设计文档
- 新增 Core 公共能力的两条路径：
  - **确定通用**：写在 `gentry-core` 里（如新拦截器、新工具类）
  - **仅当前业务用**：写在业务模块自己的 `xxx/common` 包下，不污染 core

---

## 8. 参考文档索引

| 主题 | 位置 |
|---|---|
| CLAUDE.md 总纲 | `CLAUDE.md` |
| 全局基础设施架构 | `doc/design/architecture/全局基础设施架构设计.md` |
| 各 Core 组件设计 | `doc/design/modules/core/P0~P2-*.md` |
| RBAC 模块设计 | `doc/design/modules/rbac/概要设计.md` |
| 监控模块设计 | `doc/design/modules/monitor/` |
| 协议栈设计 | `doc/design/modules/monitor/modules/协议监控/` |
| 测试用例模板 | `doc/test/rbac/测试报告模板.md` |

---

## 文档变更记录

| 版本 | 日期 | 修改人 | 变更描述 |
|---|---|---|---|
| v1.0.0 | 2026-05-05 | Claude | 初版，配合 Core 全部 10 组件落地编写 |
