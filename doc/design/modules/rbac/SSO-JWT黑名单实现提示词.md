# SSO JWT + Redis 黑名单实现提示词

> 本提示词用于指导 AI 实现车联网平台的 SSO 认证系统。请严格按步骤执行，TDD 流程：先写测试 → 红灯 → 最小实现 → 绿灯 → 重构。

---

## 一、项目背景

### 当前状态
- Spring Boot 3.2+ / JDK 21 / Sa-Token 1.38.0
- Token 模式：UUID Token（Sa-Token 默认，基于服务端 Session）
- 会话存储：Caffeine 本地缓存（无 Redis）
- 认证拦截：`SaTokenConfig` 通过 `SaInterceptor` + 自定义 `HandlerInterceptor` 恢复 `UserContext`

### 目标状态
- 新增 JWT + Redis 黑名单模式，与现有 UUID 模式共存（通过配置开关切换）
- 首期不实现完整的 SSO Server/Client 分离，仅在当前单体应用中引入 JWT Token 机制
- 为后续微服务拆分时的 SSO 铺路

### 设计文档（必读）
- 概要设计 §7.2.4 SSO 演进方案：`doc/design/modules/rbac/概要设计.md`（第 824 行起）
- 公共基础设施详细设计 §8.1：`doc/design/modules/rbac/modules/公共基础设施/后端详细设计.md`
- 用户管理详细设计 §8.1 + §10：`doc/design/modules/rbac/modules/用户管理/后端详细设计.md`
- 参考实现：`/Users/chenli/workFiles/backend/源码/sa-sso-pro`（Sa-Token SSO Pro 模式二）

---

## 二、核心文件清单（当前代码）

| 文件 | 路径 | 职责 |
|------|------|------|
| SaTokenConfig | `backend/precision-core/.../config/SaTokenConfig.java` | 拦截器注册、CORS、UserContext 恢复 |
| UserContext | `backend/precision-core/.../security/UserContext.java` | ThreadLocal（userId, tenantId） |
| AuthController | `backend/precision-business/.../user/controller/AuthController.java` | 登录/登出/用户信息/改密码 API |
| AuthService | `backend/precision-business/.../user/service/AuthService.java` | 认证服务接口 |
| AuthServiceImpl | `backend/precision-business/.../user/service/impl/AuthServiceImpl.java` | 登录核心逻辑（StpUtil.login + Session 写入） |
| LoginDTO | `backend/precision-business/.../user/dto/LoginDTO.java` | 登录请求（tenantCode, username, password） |
| LoginVO | `backend/precision-business/.../user/vo/LoginVO.java` | 登录响应（token, userInfo） |
| Sa-Token 版本 | `backend/pom.xml` 中 `sa-token.version=1.38.0` | |

---

## 三、实现步骤

### Phase 1：基础设施 — 引入 JWT 依赖 + 配置开关

**1.1 添加 Maven 依赖**（`precision-core/pom.xml`）

```xml
<!-- Sa-Token JWT 整合 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>${sa-token.version}</version>
</dependency>

<!-- Sa-Token Redis 整合（Jackson 序列化） -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-redis-jackson</artifactId>
    <version>${sa-token.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

**1.2 配置文件**（`application.yml`）

```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 1800              # JWT AccessToken 有效期 30 分钟
  active-timeout: -1         # 关闭活跃超时（JWT 自带 exp）
  is-concurrent: true
  is-share: false
  token-style: jwt-simple    # 关键：切换为 JWT 模式
  is-log: true
  jwt-secret-key: abcdefghijklmnopqrstuvwxyz0123456789   # 256-bit 密钥，生产环境外置

spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      lettuce:
        pool:
          max-active: 200
          max-idle: 10
          min-idle: 0
```

**1.3 Token 模式切换配置类**

创建 `precision-core` 中的 `TokenModeConfig.java`：

```java
package com.precision.core.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenModeConfig {

    // Sa-Token 整合 JWT（Simple 模式：Token 含 Payload 但 Session 仍在 Redis）
    @Bean
    @ConditionalOnProperty(name = "sa-token.token-style", havingValue = "jwt-simple")
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
```

**为什么用 jwt-simple 而非 jwt-stateless？**
- `jwt-stateless`：完全无状态，不创建 Session，无法踢人下线
- `jwt-simple`：Token 是 JWT 格式，但 Sa-Token 仍维护 Session（存在 Redis），支持踢人/封禁
- 我们需要 Session 能力（在线用户管理、强制下线），所以选 `jwt-simple`

### Phase 2：JWT Payload 扩展

**2.1 自定义 Payload 填充**

创建 `precision-core` 中的 `JwtPayloadHandler.java`：

```java
package com.precision.core.security;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.listener.SaTokenListener;
import org.springframework.stereotype.Component;

/**
 * 登录时将自定义数据写入 Sa-Token Session。
 * JWT Payload 由 Sa-Token 自动处理（sub=loginId, jti=tokenValue, exp=timeout）。
 * 额外的业务字段（tenantId、roles）存在 Session 中（Redis），不在 JWT 里明文暴露。
 */
@Component
public class JwtPayloadHandler implements SaTokenListener {
    // Sa-Token 事件监听，登录成功后自动触发
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        // tenantId、roles、permissions 的 Session 写入由 AuthServiceImpl.buildLoginResult() 负责
        // 此处无需额外操作
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {}
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {}
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {}
    @Override
    public void doDisable(String loginType, Object loginId, String service, long disableTime) {}
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {}
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {}
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {}
    @Override
    public void doCreateSession(String loginType, String id) {}
    @Override
    public void doLogoutSession(String loginType, String id) {}
}
```

### Phase 3：黑名单机制

**3.1 Token 黑名单服务**

创建 `precision-core` 中的 `TokenBlacklistService.java`：

```java
package com.precision.core.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;

/**
 * JWT Token 黑名单服务。
 * Key: blacklist:{tokenValue}
 * Value: 1
 * TTL: Token 剩余有效期
 */
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    /** 将 Token 加入黑名单 */
    public static void addToBlacklist(String tokenValue) {
        long ttl = StpUtil.stpLogic.getTokenTimeout(tokenValue);
        if (ttl > 0) {
            SaManager.getSaTokenDao().set(BLACKLIST_PREFIX + tokenValue, "1", ttl);
        }
    }

    /** 批量将某用户所有 Token 加入黑名单 */
    public static void blacklistAllTokensOfUser(Object loginId) {
        java.util.List<String> tokens = StpUtil.stpLogic.getTokenValueListByLoginId(loginId);
        for (String token : tokens) {
            addToBlacklist(token);
        }
    }

    /** 检查 Token 是否在黑名单 */
    public static boolean isBlacklisted(String tokenValue) {
        return SaManager.getSaTokenDao().get(BLACKLIST_PREFIX + tokenValue) != null;
    }
}
```

**3.2 黑名单校验拦截器**

修改 `SaTokenConfig.java`，在 Sa-Token 登录校验后增加黑名单检查：

```java
// 1. Sa-Token 登录校验 + JWT 黑名单检查
registry.addInterceptor(new SaInterceptor(handle -> {
    StpUtil.checkLogin();
    // JWT 黑名单检查：如果 token 已被吊销则拒绝
    String tokenValue = StpUtil.getTokenValue();
    if (tokenValue != null && TokenBlacklistService.isBlacklisted(tokenValue)) {
        StpUtil.logout();  // 清除当前会话
        throw new NotLoginException(NotLoginException.TOKEN_TIMEOUT, StpUtil.TYPE,
            "Token 已被吊销", null);
    }
}))
```

### Phase 4：业务层改造

**4.1 LoginVO 扩展**

```java
public class LoginVO {
    private String token;          // AccessToken (JWT)
    private String refreshToken;   // RefreshToken（预留，首期可为 null）
    private UserInfoVO userInfo;

    // constructor / getter / setter
}
```

**4.2 AuthServiceImpl 登出改造**

```java
@Override
public void logout() {
    String tokenValue = StpUtil.getTokenValue();
    if (tokenValue != null) {
        TokenBlacklistService.addToBlacklist(tokenValue);
    }
    StpUtil.logout();
}
```

**4.3 改密码踢设备**

在 `UserService.updatePassword()` 中追加：

```java
// 修改密码后：踢出所有在线会话 + Token 加入黑名单
TokenBlacklistService.blacklistAllTokensOfUser(userId);
StpUtil.kickout(userId);
```

**4.4 强制下线改造**

在日志管理模块的"强制下线"逻辑中追加：

```java
TokenBlacklistService.blacklistAllTokensOfUser(userId);
StpUtil.kickout(userId);
```

### Phase 5：前端适配

**5.1 前端改动极小**（因为 Sa-Token JWT 模式对前端透明）

| 改动点 | 说明 |
|--------|------|
| Token 传递方式 | 不变，仍然 `Authorization: Bearer {token}` |
| Token 存储位置 | 不变，仍然 localStorage |
| 响应结构 | 不变，仍然 `{ code: 0, data: { token, userInfo } }` |
| 401 处理 | 不变，跳转登录页 |

唯一区别：Token 值从 UUID 变为 JWT 格式（`eyJhbG...`），但前端无需解析。

---

## 四、测试要求

### 4.1 单元测试（TDD）

每个 Phase 先写测试再实现。覆盖率目标 ≥ 90%。

| 测试类 | 测试场景 |
|--------|---------|
| `TokenBlacklistServiceTest` | 加入黑名单、批量加入、检查黑名单、TTL 过期自动清理 |
| `AuthServiceTest` | JWT 模式登录、登出（jti 入黑名单）、改密码（踢设备） |
| `SaTokenConfigTest` | 拦截器链验证：未登录 → 401、已登录 → 通过、黑名单 Token → 401 |

### 4.2 集成测试

| 场景 | 步骤 | 预期 |
|------|------|------|
| 登录返回 JWT | POST /auth/login | token 以 `eyJ` 开头，可被 base64 解码 |
| 正常请求 | 携带 JWT 访问 /auth/user-info | 200，返回 userInfo |
| 登出后请求 | 登出后用原 JWT 请求 | 401（黑名单生效） |
| 改密码后请求 | 改密码后用原 JWT 请求 | 401（黑名单生效） |
| Token 过期 | 等待 30min 后请求 | 401（exp 校验失败） |

---

## 五、技术要点

### Sa-Token JWT 模式核心原理

```
登录时：
  StpUtil.login(userId) 
    → Sa-Token 签发 JWT Token（Payload: sub=userId, jti=randomUuid, exp=now+30min）
    → 同时创建 SaSession 存入 Redis（Session ID 与 JWT 的 jti 关联）

校验时：
  StpUtil.checkLogin()
    → 本地验签 JWT（HMAC-SHA256，无需网络调用）
    → 检查 Redis Session 是否存在
    → 检查黑名单

登出时：
  StpUtil.logout()
    → 删除 Redis Session
    → 我们额外：jti 加入 Redis 黑名单（TTL=Token 剩余有效期）
```

### Redis 数据分布

| Key 模式 | 用途 | 管理方 |
|----------|------|--------|
| `satoken:login:token:{tokenValue}` | Token → LoginId 映射 | Sa-Token 自动 |
| `satoken:login:session:{loginId}` | 用户 Session（含 tenantId/roles 等） | Sa-Token 自动 |
| `blacklist:{tokenValue}` | 吊销的 Token | TokenBlacklistService |

### 注意事项

1. **jwt-secret-key 必须各服务共享**：微服务拆分后所有服务使用同一个密钥
2. **Redis 连接必须稳定**：jwt-simple 模式依赖 Redis Session，Redis 宕机则无法校验
3. **不要在 JWT Payload 中放敏感信息**：tenantId 可以放，密码和权限码不放
4. **黑名单 TTL = Token 剩余有效期**：过期自动清理，不会无限增长
5. **当前阶段不实现 RefreshToken 无感续期**：AccessToken 30min 过期后需重新登录，后续迭代补充

---

## 六、不要做的事情

- **不要**实现独立的 SSO Server/Client 架构（当前单体应用不需要）
- **不要**实现 OAuth2 或 OIDC 协议
- **不要**在 JWT Payload 中存储角色/权限列表（放在 SaSession 中）
- **不要**修改前端代码（JWT 模式对前端透明）
- **不要**删除现有的 UUID Token 模式支持（保留配置切换能力）

---

## 七、验收标准

- [ ] `application.yml` 中 `token-style: jwt-simple` 配置生效
- [ ] 登录返回的 token 是 JWT 格式（`eyJ` 开头）
- [ ] 携带 JWT 请求业务接口正常返回 200
- [ ] 登出后原 JWT 请求返回 401
- [ ] 修改密码后原 JWT 请求返回 401
- [ ] 管理员强制下线后对应用户 JWT 请求返回 401
- [ ] 单元测试覆盖率 ≥ 90%
- [ ] Redis 中可观察到 `blacklist:` 前缀的 Key，且 TTL 正确递减
