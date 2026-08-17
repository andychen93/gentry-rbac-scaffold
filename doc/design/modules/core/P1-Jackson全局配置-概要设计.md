# Jackson 全局配置 概要设计

> **实现状态：✅ 已实现** — 详见 `doc/design/modules/core/P1-Jackson全局配置-详细设计.md`。

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | Jackson 全局配置概要设计 |
| 版本 | v1.0.0 |
| 创建日期 | 2026-04-12 |
| 最后更新 | 2026-04-12 |
| 负责人 | 技术经理（AI辅助） |
| 所属模块 | gentry-core |
| 优先级 | P1（必要） |

---

# 一、设计目标（What & Why）

## 是什么（What）

Jackson 全局配置是平台级的 JSON 序列化/反序列化统一规则。通过自定义 Spring Boot 的 `ObjectMapper` Bean，统一控制日期格式、Long 类型精度处理、空值策略、时区设置、未知属性策略等，确保全平台所有接口的 JSON 输入输出行为一致。

核心职责：

1. **日期格式统一**：全局统一日期时间为 `yyyy-MM-dd HH:mm:ss` 格式，日期为 `yyyy-MM-dd` 格式
2. **Long 精度保护**：JavaScript 中 Number 类型最大安全整数为 `2^53 - 1`（即 9007199254740991），超过此值的 Java Long（如雪花算法 ID）在 JSON 传输中会丢失精度。将 Long 类型序列化为 String 解决此问题
3. **空值排除**：序列化时排除 null 值字段，减少网络传输量，前端无需处理 null 判断
4. **时区统一**：全局设置 `Asia/Shanghai` 时区，避免因服务器时区不同导致的时间偏差
5. **容错处理**：反序列化时忽略未知属性，增强前后端兼容性

## 为什么（Why）

1. **前端精度问题**：项目使用雪花算法生成 ID（如 `1895012345678901234`），JavaScript 解析此值会丢失精度（尾数变为 0）。这是前后端交互中最常见且最隐蔽的 Bug 之一
2. **日期格式不统一**：不同开发人员可能使用不同的日期格式，前端需要针对每种格式做兼容处理
3. **前后端兼容性**：后端新增字段时，旧版前端发送的 JSON 不含新字段，如果严格模式会导致反序列化失败
4. **网络传输优化**：排除 null 值可减少 10%~30% 的 JSON 体积
5. **时区一致性**：车联网平台涉及 GPS 时间、报警时间等，时区不一致会导致时间显示错误

# 二、定位与上下文（Where & When）

## 在哪里（Where）

位于 `gentry-core` 模块的 `com.gentry.core.config` 包中，作为 Spring Boot 的全局 `ObjectMapper` 配置，影响所有使用 Jackson 的场景。

在系统架构中的位置：

```
┌───────────────────────────────────────────────┐
│              Jackson 全局配置                   │
│                                                │
│  ObjectMapper Bean                             │
│     ├── Spring MVC Controller  ← 序列化/反序列化│
│     ├── RestTemplate           ← 外部 API 调用 │
│     ├── WebSocket              ← 实时推送       │
│     ├── Redis Serializer       ← 缓存序列化     │
│     └── 日志输出               ← 请求体记录     │
└───────────────────────────────────────────────┘
```

依赖关系：

- **被依赖方**：全平台所有涉及 JSON 序列化/反序列化的模块
- **依赖方**：
  - Spring Boot Auto Configuration（自动配置 ObjectMapper）
  - Jackson 库（`com.fasterxml.jackson`）
  - Java 8 Date/Time 模块（`jackson-datatype-jsr310`）

## 在何时（When）

触发场景：

| 触发场景 | 操作 | 配置生效 |
|----------|------|----------|
| Controller 返回对象 | 序列化为 JSON | 日期格式、Long→String、空值排除 |
| Controller 接收 @RequestBody | 反序列化为对象 | 忽略未知属性、日期格式 |
| WebSocket 推送消息 | 序列化为 JSON | 日期格式、Long→String |
| 请求日志记录请求体 | 序列化为 JSON | 日期格式 |
| Redis 缓存序列化 | 序列化为 JSON | 日期格式、Long→String |

# 三、主要用户与交互方（Who）

## 面向谁（Who）

| 角色 | 关注点 |
|------|--------|
| **前端开发** | JSON 格式可预测（日期格式统一、Long 为字符串、无 null 字段），便于解析和展示 |
| **后端开发** | 无需在每个实体类上标注日期格式注解，无需手动处理 Long 精度问题 |
| **测试人员** | 接口返回的 JSON 格式一致，便于自动化测试断言 |
| **第三方集成** | JSON 格式规范，便于对接 |

## 与其他组件的交互

| 交互组件 | 交互方式 | 说明 |
|----------|----------|------|
| Spring MVC | 自动配置 | Spring Boot 自动使用自定义的 ObjectMapper |
| `R<>` 响应类 | 序列化 | 统一响应通过全局 ObjectMapper 序列化 |
| WebSocket | 序列化 | 实时推送数据通过全局 ObjectMapper 序列化 |
| `RequestLogFilter` | 序列化 | 日志记录请求体时使用 |

# 四、实现思路（How）

## 核心技术方案

### 4.1 配置类结构

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. 注册 Java 8 Date/Time 模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDate.class,
            new LocalDateSerializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class,
            new LocalTimeSerializer(
                DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDate.class,
            new LocalDateDeserializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class,
            new LocalTimeDeserializer(
                DateTimeFormatter.ofPattern("HH:mm:ss")));
        mapper.registerModule(javaTimeModule);

        // 2. Long → String 序列化
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, new LongToStringSerializer());
        longModule.addSerializer(Long.TYPE, new LongToStringSerializer());
        mapper.registerModule(longModule);

        // 3. 排除 null 值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 4. 时区
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 5. 忽略未知属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 6. 禁用日期时间戳格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
```

### 4.2 Long → String 自定义序列化器

```java
public class LongToStringSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value != null) {
            gen.writeString(value.toString());
        }
    }
}
```

**效果示例：**

```json
// 配置前
{
  "id": 1895012345678901234,   // JavaScript 解析丢失精度
  "tenantId": 1001,
  "plateNumber": "京A12345",
  "createTime": [2026, 4, 12, 10, 30, 0]  // 数组格式
}

// 配置后
{
  "id": "1895012345678901234",  // 字符串，精度不丢失
  "tenantId": "1001",
  "plateNumber": "京A12345",
  "createTime": "2026-04-12 10:30:00"  // 可读格式
}
```

### 4.3 特殊场景处理

| 场景 | 处理方式 | 说明 |
|------|----------|------|
| Long 类型的 ID 字段 | 序列化为 String | 前端使用 String 类型接收 |
| Long 类型的数值字段（如金额） | 需要特殊处理 | 如果有金额等需要数值计算的 Long 字段，可使用 `@JsonSerialize(using = NumberSerializer.class)` 单独覆盖 |
| BigDecimal 字段 | 保持默认 | 不转换为 String，前端可正常数值运算 |
| 枚举字段 | 使用 `@JsonFormat(shape = JsonFormat.Shape.STRING)` | 序列化为枚举名称 |
| 日期字段 | yyyy-MM-dd HH:mm:ss | LocalDateTime 和 Date 统一格式 |

### 4.4 兼容性考虑

| 兼容项 | 处理方式 |
|--------|----------|
| 前端旧代码 expecting Number | 前端统一改造，使用 String 类型接收 ID |
| 第三方 API 返回数值型 ID | RestTemplate 使用独立的 ObjectMapper，不做 Long→String 转换 |
| 数据库 BIGINT 主键 | Java 实体使用 Long 类型，Jackson 负责序列化转换 |
| 前端传参 | @RequestBody 接收时 String 类型的 ID 可自动绑定到 Long 字段（Jackson 的 String→Long 反序列化天然支持） |

## 关键类和接口设计

| 类名 | 包路径 | 职责 |
|------|--------|------|
| `JacksonConfig` | `com.gentry.core.config` | 全局 ObjectMapper 配置 |
| `LongToStringSerializer` | `com.gentry.core.config` | Long→String 自定义序列化器 |

## 与其他组件的协作关系

```
┌────────────────────────────────────────────────────────────┐
│                    Jackson 全局配置                          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ ObjectMapper Bean (@Primary)                         │  │
│  │                                                       │  │
│  │  ┌──────────────────┐  ┌───────────────────────┐    │  │
│  │  │ JavaTimeModule   │  │ LongToStringSerializer │    │  │
│  │  │ LocalDateTime →  │  │ Long → String          │    │  │
│  │  │ "yyyy-MM-dd      │  │ (雪花ID精度保护)       │    │  │
│  │  │  HH:mm:ss"       │  │                        │    │  │
│  │  └──────────────────┘  └───────────────────────┘    │  │
│  │                                                       │  │
│  │  ┌──────────────────┐  ┌───────────────────────┐    │  │
│  │  │ NON_NULL         │  │ Asia/Shanghai         │    │  │
│  │  │ 排除 null 值      │  │ 统一时区              │    │  │
│  │  └──────────────────┘  └───────────────────────┘    │  │
│  │                                                       │  │
│  │  ┌──────────────────────────────────────────────┐   │  │
│  │  │ FAIL_ON_UNKNOWN_PROPERTIES = false           │   │  │
│  │  │ 忽略未知属性，增强兼容性                       │   │  │
│  │  └──────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  被使用于：                                                  │
│  ├── Spring MVC Controller（自动注入）                       │
│  ├── WebSocket Handler                                     │
│  ├── RequestLogFilter                                      │
│  └── Redis Cache Serializer                                │
└────────────────────────────────────────────────────────────┘
```

# 五、预期效果与验收标准

## 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 序列化性能影响 | < 5% | 相比默认 ObjectMapper 的额外开销 |
| Long→String 转换耗时 | 可忽略 | toString() 操作 |
| JSON 体积减少 | 10%~30% | 排除 null 字段后的体积缩减 |

## 功能验收标准

| 编号 | 验收项 | 验证方式 |
|------|--------|----------|
| J-001 | LocalDateTime 序列化为 yyyy-MM-dd HH:mm:ss | 返回含日期字段的接口，验证格式 |
| J-002 | LocalDate 序列化为 yyyy-MM-dd | 返回含日期字段的接口，验证格式 |
| J-003 | Long 类型序列化为 String | 返回含 ID 字段的接口，验证 ID 为字符串 |
| J-004 | null 字段不出现在 JSON 中 | 返回含 null 字段的对象，验证 JSON 无 null |
| J-005 | 反序列化忽略未知属性 | 发送含额外字段的 JSON，验证不报错 |
| J-006 | 前端 String 传参可绑定到 Long | 前端发送 String 类型 ID，后端 Long 字段正常接收 |
| J-007 | 雪花 ID 精度不丢失 | 使用超过 2^53 的 ID，验证前后端精度一致 |
| J-008 | 时区统一为 Asia/Shanghai | 不同服务器部署，验证时间格式一致 |
| J-009 | 大数字（BigDecimal）保持数值格式 | 金额等字段验证仍为数值而非字符串 |

# 六、配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `precision.jackson.date-format` | `yyyy-MM-dd HH:mm:ss` | 日期时间格式 |
| `precision.jackson.date-format-short` | `yyyy-MM-dd` | 短日期格式 |
| `precision.jackson.time-zone` | `Asia/Shanghai` | 全局时区 |
| `precision.jackson.long-to-string` | `true` | 是否将 Long 序列化为 String |
| `precision.jackson.exclude-null` | `true` | 是否排除 null 值 |
| `precision.jackson.fail-on-unknown` | `false` | 是否对未知属性报错 |

# 七、参考文档

| 文档 | 说明 |
|------|------|
| Jackson 官方文档 - ObjectMapper | 序列化配置 |
| Spring Boot 3.2 官方文档 - JSON | 自定义 Jackson 配置 |
| jackson-datatype-jsr310 | Java 8 Date/Time 支持 |
| JavaScript Number.MAX_SAFE_INTEGER | Long 精度问题背景 |
| CCF 微服务开发白皮书 v2.1 附录 B.1 | 概要设计模板 |
