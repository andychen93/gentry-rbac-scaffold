# Redis 监控功能实现提示词

> 本提示词用于指导 AI 实现 Redis 监控功能。严格按步骤执行，TDD 流程：先写测试 → 红灯 → 最小实现 → 绿灯 → 重构。

---

## 一、项目背景

### 当前状态
- Spring Boot 3.2.5 / JDK 21 / Sa-Token 1.38.0
- Redis 已接入：`sa-token-redis-jackson` + `spring-boot-starter-data-redis`（Lettuce 连接池）
- Redis 配置：`application.yml` 中 `spring.data.redis.*` 已配好（127.0.0.1:6379）
- 认证：Sa-Token JWT（jwt-simple），拦截器在 `SaTokenConfig`
- 父 POM：`backend/pom.xml`，现有模块 precision-core / precision-business / precision-protocol / precision-start

### 目标状态
- 新建 `precision-monitor` 模块，作为监控功能的独立承载模块
- Redis 监控功能包路径：`com.precision.monitor.redis`
- 通过 `precision-start` 聚合启动

### 设计文档（必读）
- 后端详细设计：`doc/design/modules/monitor/modules/Redis监控/后端详细设计.md`
- 前端详细设计：`doc/design/modules/monitor/modules/Redis监控/前端详细设计.md`

---

## 二、模块结构

### 新增 Maven 模块

在 `backend/` 下新建 `precision-monitor` 子模块：

```
backend/
├── precision-core/              # 已有：全局基础设施
├── precision-business/          # 已有：业务模块（RBAC 等）
├── precision-protocol/          # 已有：协议模块
├── precision-monitor/           # 🆕 新建：监控模块
│   ├── pom.xml
│   └── src/main/java/com/precision/monitor/
│       └── redis/
│           ├── controller/
│           │   └── RedisMonitorController.java
│           ├── service/
│           │   ├── RedisMonitorService.java
│           │   ├── RedisKeyService.java
│           │   └── impl/
│           │       ├── RedisMonitorServiceImpl.java
│           │       └── RedisKeyServiceImpl.java
│           ├── vo/
│           │   ├── RedisMonitorVO.java
│           │   ├── RedisInfoVO.java
│           │   ├── RedisCommandStatVO.java
│           │   ├── RedisKeyDefineVO.java
│           │   └── RedisKeyVO.java
│           └── enums/
│               └── RedisKeyDataType.java
├── precision-start/             # 已有：需要新增 precision-monitor 依赖
└── pom.xml                      # 已有：需要新增 module 声明
```

---

## 三、实现步骤

### Phase 1：Maven 模块骨架

**1.1 创建 `precision-monitor/pom.xml`**

```xml
<?xml version="1.0.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.precision</groupId>
        <artifactId>precision-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>precision-monitor</artifactId>
    <name>Precision Monitor</name>

    <dependencies>
        <dependency>
            <groupId>com.precision</groupId>
            <artifactId>precision-core</artifactId>
        </dependency>
        <!-- Spring Data Redis（已由 precision-core 传递 sa-token-redis-jackson，但显式声明用于直接操作 Redis） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

**1.2 修改父 POM `backend/pom.xml`**

在 `<modules>` 中新增：
```xml
<module>precision-monitor</module>
```

在 `<dependencyManagement>` 中新增：
```xml
<dependency>
    <groupId>com.precision</groupId>
    <artifactId>precision-monitor</artifactId>
    <version>${project.version}</version>
</dependency>
```

**1.3 修改 `precision-start/pom.xml`**

新增依赖：
```xml
<dependency>
    <groupId>com.precision</groupId>
    <artifactId>precision-monitor</artifactId>
</dependency>
```

### Phase 2：VO 对象（纯数据结构，无依赖）

创建以下 VO 类，包路径 `com.precision.monitor.redis.vo`：

**RedisInfoVO.java**
```java
@Data
public class RedisInfoVO {
    private String redisVersion;
    private String redisMode;        // standalone / sentinel / cluster
    private Long uptimeInSeconds;
    private Integer connectedClients;
    private Long usedMemory;         // bytes
    private Long maxMemory;          // bytes，0=无限制
    private Double usedMemoryPercent; // %
    private Long totalKeys;
    private Long expiresKeys;
    private Long avgTtl;             // ms
    private String aofEnabled;       // yes/no
    private String rdbLastSaveTime;
}
```

**RedisCommandStatVO.java**
```java
@Data
public class RedisCommandStatVO {
    private String name;
    private Long calls;
    private Long usec;
    private Long usecPerCall;
}
```

**RedisMonitorVO.java**
```java
@Data
public class RedisMonitorVO {
    private RedisInfoVO info;
    private Long dbSize;
    private List<RedisCommandStatVO> commandStats;
}
```

**RedisKeyDefineVO.java**
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisKeyDefineVO {
    private String keyType;
    private String keyTemplate;
    private String description;
    private Long timeout;
}
```

**RedisKeyVO.java**
```java
@Data
public class RedisKeyVO {
    private String key;
    private String type;   // string / hash / list / set / zset
    private String value;  // 格式化后的值（截断到 500 字符）
    private Long ttl;      // 剩余秒数，-1=永不过期，-2=已过期/不存在
}
```

### Phase 3：Service 层（核心逻辑）

**3.1 RedisMonitorService.java（接口）**

```java
public interface RedisMonitorService {
    RedisMonitorVO getMonitorInfo();
    List<RedisKeyDefineVO> getKeyDefines();
}
```

**3.2 RedisMonitorServiceImpl.java**

核心实现要点：

```java
@Service
public class RedisMonitorServiceImpl implements RedisMonitorService {

    private final StringRedisTemplate redisTemplate;

    // 注入 StringRedisTemplate（Spring Boot 自动装配）

    @Override
    public RedisMonitorVO getMonitorInfo() {
        // 1. 获取 Properties 对象（等价于 INFO all）
        Properties info = redisTemplate.execute((RedisCallback<Properties>) con ->
            con.serverCommands().info()
        );

        // 2. 解析 info → RedisInfoVO
        RedisInfoVO infoVO = parseInfo(info);

        // 3. 获取命令统计
        Properties commandStats = redisTemplate.execute((RedisCallback<Properties>) con ->
            con.serverCommands().info("commandstats")
        );
        List<RedisCommandStatVO> stats = parseCommandStats(commandStats);

        // 4. 获取 dbSize
        Long dbSize = redisTemplate.execute((RedisCallback<Long>) con ->
            con.serverCommands().dbSize()
        );

        // 5. 组装返回
        RedisMonitorVO vo = new RedisMonitorVO();
        vo.setInfo(infoVO);
        vo.setDbSize(dbSize);
        vo.setCommandStats(stats);
        return vo;
    }

    // INFO 解析核心方法
    private RedisInfoVO parseInfo(Properties info) {
        RedisInfoVO vo = new RedisInfoVO();
        vo.setRedisVersion(info.getProperty("redis_version"));
        vo.setRedisMode(info.getProperty("redis_mode"));
        vo.setUptimeInSeconds(parseLong(info.getProperty("uptime_in_seconds")));
        vo.setConnectedClients(parseInteger(info.getProperty("connected_clients")));
        vo.setUsedMemory(parseLong(info.getProperty("used_memory")));
        vo.setMaxMemory(parseLong(info.getProperty("maxmemory")));
        // 内存使用率 = used / max * 100，max=0 时为 0%
        long maxMemory = vo.getMaxMemory() != null ? vo.getMaxMemory() : 0L;
        long usedMemory = vo.getUsedMemory() != null ? vo.getUsedMemory() : 0L;
        if (maxMemory > 0) {
            vo.setUsedMemoryPercent(Math.round(usedMemory * 10000.0 / maxMemory) / 100.0);
        } else {
            vo.setUsedMemoryPercent(0.0);
        }

        // Keyspace 信息解析：db0:keys=1523,expires=800,avg_ttl=1200000
        String keyspace = info.getProperty("db0");
        if (keyspace != null) {
            parseKeyspace(keyspace, vo);
        }

        vo.setAofEnabled(info.getProperty("aof_enabled"));
        vo.setRdbLastSaveTime(info.getProperty("rdb_last_save_time"));
        return vo;
    }

    // Keyspace 解析
    private void parseKeyspace(String keyspace, RedisInfoVO vo) {
        // db0:keys=1523,expires=800,avg_ttl=1200000
        String[] parts = keyspace.split(",");
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                switch (kv[0]) {
                    case "keys" -> vo.setTotalKeys(parseLong(kv[1]));
                    case "expires" -> vo.setExpiresKeys(parseLong(kv[1]));
                    case "avg_ttl" -> vo.setAvgTtl(parseLong(kv[1]));
                }
            }
        }
    }

    // 命令统计解析
    // INFO commandstats 返回格式：cmdstat_get:calls=50000,usec=120000,usec_per_call=2.40
    private List<RedisCommandStatVO> parseCommandStats(Properties stats) {
        if (stats == null) return Collections.emptyList();
        return stats.entrySet().stream()
            .filter(e -> e.getKey().toString().startsWith("cmdstat_"))
            .map(e -> {
                String cmdName = e.getKey().toString().substring("cmdstat_".length());
                String value = e.getValue().toString();
                // calls=50000,usec=120000,usec_per_call=2.40
                RedisCommandStatVO stat = new RedisCommandStatVO();
                stat.setName(cmdName);
                for (String part : value.split(",")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2) {
                        switch (kv[0]) {
                            case "calls" -> stat.setCalls(parseLong(kv[1]));
                            case "usec" -> stat.setUsec(parseLong(kv[1]));
                            case "usec_per_call" -> stat.setUsecPerCall(parseLong(kv[1].split("\\.")[0]));
                        }
                    }
                }
                return stat;
            })
            .sorted((a, b) -> Long.compare(
                b.getCalls() != null ? b.getCalls() : 0,
                a.getCalls() != null ? a.getCalls() : 0))
            .collect(Collectors.toList());
    }
}
```

**3.3 RedisKeyService.java（接口）**

```java
public interface RedisKeyService {
    PageResult<RedisKeyVO> scanKeys(String pattern, int pageNum, int pageSize);
    RedisKeyVO getKeyValue(String key);
    boolean deleteKey(String key);
}
```

**3.4 RedisKeyServiceImpl.java**

核心实现要点：

```java
@Service
public class RedisKeyServiceImpl implements RedisKeyService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<RedisKeyVO> scanKeys(String pattern, int pageNum, int pageSize) {
        // 1. 使用 SCAN 游标遍历匹配 pattern 的 Key
        Set<String> keys = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
            .match(pattern)
            .count(100)
            .build();

        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        try (RedisConnection connection = factory.getConnection()) {
            Cursor<byte[]> cursor = connection.keyCommands().scan(options);
            while (cursor.hasNext() && keys.size() < pageNum * pageSize) {
                keys.add(new String(cursor.next()));
            }
        }

        // 2. 分页截取
        List<String> keyList = new ArrayList<>(keys);
        int fromIndex = Math.min((pageNum - 1) * pageSize, keyList.size());
        int toIndex = Math.min(fromIndex + pageSize, keyList.size());
        List<String> pageKeys = keyList.subList(fromIndex, toIndex);

        // 3. 批量获取类型和 TTL
        List<RedisKeyVO> result = pageKeys.stream().map(k -> {
            RedisKeyVO vo = new RedisKeyVO();
            vo.setKey(k);
            DataType dataType = redisTemplate.type(k);
            vo.setType(dataType.code());
            vo.setTtl(redisTemplate.getExpire(k, TimeUnit.SECONDS));
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(result, (long) keys.size(), pageNum, pageSize);
    }

    @Override
    public RedisKeyVO getKeyValue(String key) {
        RedisKeyVO vo = new RedisKeyVO();
        vo.setKey(key);

        DataType dataType = redisTemplate.type(key);
        vo.setType(dataType.code());
        vo.setTtl(redisTemplate.getExpire(key, TimeUnit.SECONDS));

        String value = switch (dataType) {
            case STRING -> redisTemplate.opsForValue().get(key);
            case HASH -> {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                yield toJsonString(entries);
            }
            case LIST -> {
                List<String> list = redisTemplate.opsForList().range(key, 0, -1);
                yield toJsonString(list);
            }
            case SET -> {
                Set<String> set = redisTemplate.opsForSet().members(key);
                yield toJsonString(set);
            }
            case ZSET -> {
                Set<String> zset = redisTemplate.opsForZSet().range(key, 0, -1);
                yield toJsonString(zset);
            }
            default -> "unsupported type: " + dataType.code();
        };

        // 截断大 Value，防止前端渲染卡顿
        if (value != null && value.length() > 500) {
            value = value.substring(0, 500) + "...(截断，总长度:" + value.length() + ")";
        }
        vo.setValue(value);
        return vo;
    }

    @Override
    public boolean deleteKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // JSON 格式化辅助
    private String toJsonString(Object obj) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
}
```

**3.5 KeyDefine 常量类**

```java
public class RedisKeyDefines {

    private static final List<RedisKeyDefineVO> DEFINES = List.of(
        new RedisKeyDefineVO("token_mapping", "satoken:login:token:*", "Token → LoginId 映射", 1800L),
        new RedisKeyDefineVO("user_session", "satoken:login:session:*", "用户 Session", 1800L),
        new RedisKeyDefineVO("jwt_blacklist", "blacklist:*", "JWT 黑名单", -2L),
        new RedisKeyDefineVO("token_timeout", "satoken:timeout:*", "Token 过期时间辅助", 1800L)
    );

    public static List<RedisKeyDefineVO> getAll() {
        return DEFINES;
    }
}
```

### Phase 4：Controller 层

**RedisMonitorController.java**

```java
@RestController
@RequestMapping("/api/v1/monitor/redis")
public class RedisMonitorController {

    private final RedisMonitorService monitorService;
    private final RedisKeyService keyService;

    // 构造注入

    @GetMapping("/info")
    public R<RedisMonitorVO> getMonitorInfo() {
        return R.ok(monitorService.getMonitorInfo());
    }

    @GetMapping("/key-defines")
    public R<List<RedisKeyDefineVO>> getKeyDefines() {
        return R.ok(monitorService.getKeyDefines());
    }

    @GetMapping("/keys")
    public R<PageResult<RedisKeyVO>> scanKeys(
            @RequestParam String pattern,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        return R.ok(keyService.scanKeys(pattern, pageNum, pageSize));
    }

    @GetMapping("/keys/{key}/value")
    public R<RedisKeyVO> getKeyValue(@PathVariable String key) {
        return R.ok(keyService.getKeyValue(key));
    }

    @DeleteMapping("/keys/{key}")
    public R<Void> deleteKey(@PathVariable String key) {
        keyService.deleteKey(key);
        return R.ok();
    }
}
```

### Phase 5：权限配置

监控接口需要在 RBAC 系统中注册权限。需要执行以下 SQL：

```sql
-- 新增监控菜单（挂载在系统监控目录下，假设 parent_id 为系统监控目录 ID）
INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort, visible, status, icon)
VALUES (9001, 0, '系统监控', '/monitor', NULL, 1, 90, 1, 1, 'monitor');

INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort, visible, status, icon)
VALUES (9002, 9001, 'Redis 监控', '/monitor/redis', 'monitor:redis:info', 2, 1, 1, 1, 'redis');

-- Redis 监控按钮权限
INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort, visible, status)
VALUES (9010, 9002, 'Key 列表查询', NULL, 'monitor:redis:key:list', 3, 1, 1, 1);

INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort, visible, status)
VALUES (9011, 9002, 'Key 值查看', NULL, 'monitor:redis:key:query', 3, 2, 1, 1);

INSERT INTO sys_menu (id, parent_id, name, path, permission, type, sort, visible, status)
VALUES (9012, 9002, 'Key 删除', NULL, 'monitor:redis:key:delete', 3, 3, 1, 1);

-- 超级管理员角色授权（假设 role_id=-1 为超级管理员）
-- 具体关联 sys_role_menu 表
```

> 注意：实际 ID 和 parent_id 需要根据当前系统已有的菜单树确定，以上 SQL 仅供参考。

---

## 四、测试要求

### 4.1 单元测试（TDD）

每个 Phase 先写测试再实现。覆盖率目标 ≥ 90%。

| 测试类 | 测试场景 |
|--------|---------|
| `RedisMonitorServiceImplTest` | INFO 解析 → RedisInfoVO、命令统计解析、dbSize、内存使用率计算、keyspace 解析 |
| `RedisKeyServiceImplTest` | SCAN 分页、各类型 Value 获取（string/hash/list/set/zset）、Value 截断、删除 Key |
| `RedisMonitorControllerTest` | 5 个接口的 MockMvc 测试：正常响应、参数校验、权限校验 |

### 4.2 测试策略

- `RedisMonitorServiceImpl` 和 `RedisKeyServiceImpl` 使用 `@Mock StringRedisTemplate` + `@Mock RedisConnection`
- INFO 解析是纯字符串处理，可以构造 Properties 对象直接测试
- Controller 使用 `@WebMvcTest` + `@MockBean`

### 4.3 关键测试用例

```java
// 1. INFO 解析测试
@Test
void parseInfo_normalInfo_allFieldsSet() {
    Properties info = new Properties();
    info.setProperty("redis_version", "7.2.4");
    info.setProperty("redis_mode", "standalone");
    info.setProperty("used_memory", "1048576");
    info.setProperty("maxmemory", "1073741824");
    info.setProperty("db0", "keys=1523,expires=800,avg_ttl=1200000");

    RedisInfoVO result = parseInfo(info);

    assertEquals("7.2.4", result.getRedisVersion());
    assertEquals(0.1, result.getUsedMemoryPercent(), 0.01);
    assertEquals(1523L, result.getTotalKeys());
}

// 2. 命令统计解析测试
@Test
void parseCommandStats_normalStats_sortedByCalls() {
    Properties stats = new Properties();
    stats.setProperty("cmdstat_get", "calls=50000,usec=120000,usec_per_call=2.40");
    stats.setProperty("cmdstat_set", "calls=30000,usec=90000,usec_per_call=3.00");

    List<RedisCommandStatVO> result = parseCommandStats(stats);

    assertEquals(2, result.size());
    assertEquals("get", result.get(0).getName());  // calls 更多的排前面
}

// 3. Value 截断测试
@Test
void getKeyValue_largeValue_truncated() {
    String longValue = "a".repeat(1000);
    when(redisTemplate.type("bigkey")).thenReturn(DataType.STRING);
    when(redisTemplate.opsForValue().get("bigkey")).thenReturn(longValue);

    RedisKeyVO result = keyService.getKeyValue("bigkey");

    assertTrue(result.getValue().contains("截断"));
}
```

---

## 五、技术要点

### 核心依赖关系

```
precision-monitor  →  precision-core
                  →  spring-boot-starter-data-redis
                  →  lombok
```

- `StringRedisTemplate` 由 `spring-boot-starter-data-redis` 自动装配，无需额外配置
- Redis 连接配置复用 `application.yml` 中已有的 `spring.data.redis.*`
- Sa-Token 权限校验通过 `SaTokenConfig` 全局拦截器自动生效，无需额外配置

### INFO 命令返回值解析

Redis `INFO all` 命令返回格式为 `key:value` 文本行，Spring Data Redis 封装为 `Properties` 对象。关键字段：

| INFO 字段 | 含义 | 示例值 |
|-----------|------|--------|
| `redis_version` | Redis 版本 | `7.2.4` |
| `redis_mode` | 运行模式 | `standalone` |
| `uptime_in_seconds` | 运行时长 | `86400` |
| `connected_clients` | 连接数 | `12` |
| `used_memory` | 已用内存（bytes） | `1048576` |
| `maxmemory` | 最大内存（bytes） | `1073741824` |
| `db0` | 数据库 Key 统计 | `keys=1523,expires=800,avg_ttl=1200000` |
| `aof_enabled` | AOF 开关 | `1` |

### SCAN 命令注意事项

1. **SCAN 是非阻塞的**：通过游标逐步遍历，不会像 `KEYS *` 那样阻塞 Redis
2. **COUNT 只是建议值**：Redis 不保证每次返回 COUNT 条结果
3. **pattern 使用 `MATCH`**：在 SCAN 命令中加 MATCH 过滤
4. **分页逻辑**：先收集所有匹配 Key 到 Set，再内存分页（Redis 不支持服务端分页）

---

## 六、不要做的事情

- **不要**修改 `precision-core` 或 `precision-business` 中的任何现有代码
- **不要**在 `precision-business` 中添加监控相关代码（监控是独立模块）
- **不要**引入额外的 Redis 客户端（如 Jedis、Lettuce 直接使用），统一用 `StringRedisTemplate`
- **不要**做 Redis 配置修改功能（只读监控）
- **不要**做历史数据存储和趋势图（后续接入 Prometheus + Grafana）
- **不要**实现 Redis 集群管理功能

---

## 七、验收标准

- [ ] `precision-monitor` 模块创建并编译通过
- [ ] `precision-start` 依赖 `precision-monitor` 并正常启动
- [ ] `GET /api/v1/monitor/redis/info` 返回 Redis 监控信息（含 info、dbSize、commandStats）
- [ ] `GET /api/v1/monitor/redis/key-defines` 返回系统预定义的 Key 模板列表
- [ ] `GET /api/v1/monitor/redis/keys?pattern=blacklist:*` 返回匹配的 Key 列表（含 type、ttl）
- [ ] `GET /api/v1/monitor/redis/keys/{key}/value` 返回 Key 的值（按类型格式化）
- [ ] `DELETE /api/v1/monitor/redis/keys/{key}` 删除指定 Key
- [ ] 未登录访问返回 401
- [ ] 无权限访问返回 403
- [ ] 单元测试覆盖率 ≥ 90%
