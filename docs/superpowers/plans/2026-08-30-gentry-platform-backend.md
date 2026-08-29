# Gentry 平台化改造 — 后端实施计划（P1–P3）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 gentry-core / gentry-business / gentry-monitor 改造为三个 Spring Boot Starter + BOM，gentry-start 作为狗粮消费它们，实现「新项目引依赖而不是 fork」。

**Architecture:** 每个 starter 用 `@AutoConfiguration` + 定向 `@ComponentScan` 自动装配（注册进 `AutoConfiguration.imports`）；Flyway 迁移挪入 rbac-starter 的独立 classpath 目录（版本段 V1–V999 平台保留），消费项目自 V1000 起；主类移包 `com.gentry` → `com.gentry.start` 消除根包扫描依赖。设计文档：`doc/design/architecture/平台化改造概要设计.md`。

**Tech Stack:** Spring Boot 3.2.5 / JDK 21 / MyBatis-Flex 1.11.6 / Sa-Token 1.38 / Flyway 9.22 / JUnit5 + Mockito

**工作目录：** 后端所有路径相对 `backend/`。所有 `mvn` 命令在 `backend/` 下执行。

**关键背景（零上下文工程师必读）：**

- 现状 4 模块：`gentry-core`（横切，21 个配置/组件类）→ `gentry-business`（rbac 9 个子包 + notification 15 个文件，176 个 Java 文件）→ `gentry-monitor`（Redis 监控）→ `gentry-start`（主类 `com.gentry.GentryApplication` + 14 个 `*ApiIT` + Flyway 迁移 + application*.yml）。
- 主类在 `com.gentry` 根包，所有 Bean 靠 `@SpringBootApplication` 包扫描兜住 —— 这是 starter 化要消灭的隐式依赖。
- `@MapperScan("com.gentry.**.mapper")` 在主类上；Mapper XML 在 `gentry-business/src/main/resources/mapper/`（12 个 XML，`mybatis-flex.mapper-locations: classpath*:mapper/**/*.xml` 加载，随 jar 分发无碍）。
- **Flyway 依赖（flyway-core + flyway-mysql）目前只在 `gentry-start/pom.xml`**，gentry-business 没有 —— 本计划 Task 3 会把它挪进 rbac-starter（平台契约，消费方不该自配）。
- 基线测试：`mvn clean test` 355 个全绿（core ~130 / business ~70 / start IT + 单测）。**必须 clean**（IDE 毒 class，见用户 memory）；**必须 `-Dspring.datasource.password=CHENLIchenli321!`**（本机原生 MySQL）；IT 还需要本地 MySQL + Redis 已起。
- `mvn -pl <module> test` 不可信（会从 ~/.m2 拿旧产物），改 core 后必须全 reactor `mvn clean test`。
- Flyway 迁移：common/ 14 个（V2–V15），三个厂商目录各有 V1 建表，共 17 个。已发布的迁移只挪目录**不可改内容**（checksum 只算文件内容 CRC32，与位置无关，已初始化库无感）。
- `gentry.*` 配置命名空间已存在（i18n/captcha/login-security/password/log），全部保留不动。
- Git 分支：`feature/gentry-platform`（worktree `.claude/worktrees/gentry-platform`）。
- 已核实无坑的部分：core/business/monitor 无 spring.factories / META-INF 装配 / @Profile；父 pom jacoco 全模块继承不引模块名；surefire `*IT` include 只在 gentry-start pom（模块不改名）；scripts 引用的都是 gentry-start 路径（不改名）。

**总验证门槛（每个 Task 完成后跑）：**

```bash
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

（clean 必须：跨模块 git mv + 更名后，旧 `~/.m2` 里 `com.gentry:gentry-business` 等旧坐标产物与残留 target 是脏 class 高发源。）

---

## Phase 1：core 自动装配（gentry-core → gentry-core-spring-boot-starter）

### Task 1: core 自动装配类 + imports 注册

**Files:**
- Create: `gentry-core/src/main/java/com/gentry/core/config/GentryCoreAutoConfiguration.java`
- Create: `gentry-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `gentry-core/src/test/java/com/gentry/core/config/GentryCoreAutoConfigurationTest.java`

- [ ] **Step 1: 写失败的自动装配测试**

```java
package com.gentry.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配契约测试。全栈行为（Bean 真正起作用）由 gentry-start 的 14 个 IT 覆盖，
 * 这里只锁两个静态契约：imports 登记 + 扫描包正确。
 */
class GentryCoreAutoConfigurationTest {

    @Test
    void imports文件_登记了core自动装配类() throws Exception {
        String content = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
        assertThat(content.trim()).isEqualTo("com.gentry.core.config.GentryCoreAutoConfiguration");
    }

    @Test
    void 注解_扫描com_gentry_core包() {
        org.springframework.context.annotation.ComponentScan scan =
                GentryCoreAutoConfiguration.class.getAnnotation(org.springframework.context.annotation.ComponentScan.class);
        assertThat(scan).isNotNull();
        assertThat(scan.value()).containsExactly("com.gentry.core");
    }
}
```

（不用 ApplicationContextRunner 冒烟：core 的 Bean 依赖 Redis/数据源/sa-token 自动配置，runner 里排除这些后剩下的可断言面很小、维护成本高于收益；「装配真的工作」由 14 个全栈 IT 把关。）

- [ ] **Step 2: 跑测试确认失败**

```bash
cd backend && mvn -pl gentry-core test -Dtest=GentryCoreAutoConfigurationTest
```

预期：编译失败（类不存在）。

- [ ] **Step 3: 最小实现**

`GentryCoreAutoConfiguration.java`：

```java
package com.gentry.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * gentry-core 自动装配入口。
 *
 * <p>starter 化前，core 的 Bean 依赖消费方主类放在 {@code com.gentry} 根包下被包扫描兜住；
 * starter 化后由本类定向扫描注册，消费方主类可在任意包。
 * core 内配置类之间有构造注入依赖（如 SaTokenConfig ← GentryLocaleResolver 的
 * ObjectProvider），逐个转 @Bean 声明容易制造循环，定向 ComponentScan 保持原语义。</p>
 */
@AutoConfiguration
@ComponentScan("com.gentry.core")
public class GentryCoreAutoConfiguration {
}
```

`AutoConfiguration.imports`（单行，UTF-8，无 BOM）：

```
com.gentry.core.config.GentryCoreAutoConfiguration
```

- [ ] **Step 4: 跑测试确认通过 + 全量回归**

```bash
cd backend && mvn -pl gentry-core test -Dtest=GentryCoreAutoConfigurationTest && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

预期：新增 2 个测试绿，全量绿。此阶段主类还在 `com.gentry` 根包，imports 通道 + 包扫描双通道并存 —— 同一配置类被双发现时 ConfigurationClassParser 按 class 去重、组件按 bean 名去重，不会双重注册（已核实 Spring 6.1 行为）。

- [ ] **Step 5: Commit**

```bash
git add backend/gentry-core
git commit -m "feat(core): 新增 GentryCoreAutoConfiguration 自动装配入口"
```

### Task 2: gentry-core 更名为 gentry-core-spring-boot-starter

**Files:**
- Modify: `backend/pom.xml`（modules 1 处 + dependencyManagement 1 处，均已核实只各出现 1 次）
- Modify: `backend/gentry-core/pom.xml`（artifactId）
- Modify: `backend/gentry-business/pom.xml`、`backend/gentry-monitor/pom.xml`、`backend/gentry-start/pom.xml`（依赖坐标各 1 处）
- Rename: `backend/gentry-core/` → `backend/gentry-core-spring-boot-starter/`（`git mv`）

- [ ] **Step 1: git mv 目录并改坐标**

```bash
cd backend
git mv gentry-core gentry-core-spring-boot-starter
```

改 4 个 pom（每个文件里旧名 `gentry-core` 精确替换为 `gentry-core-spring-boot-starter`；**注意别误改 `gentry-core-spring-boot-starter` 已改过的行**，建议手工逐处改）：

1. `gentry-core-spring-boot-starter/pom.xml`：`<artifactId>gentry-core</artifactId>` → 新名。
2. 父 `pom.xml`：`<module>` 行 + `dependencyManagement` 的 `<artifactId>` 行。
3. 三个下游模块 pom 的依赖 `<artifactId>`。

- [ ] **Step 2: 清理旧产物 + 全量构建验证**

```bash
rm -rf gentry-core-spring-boot-starter/target   # git mv 会带上旧 target，必须清
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

预期：全绿（纯更名，无行为变化）。

- [ ] **Step 3: Commit**

```bash
git add -A backend
git commit -m "refactor(core): 模块更名为 gentry-core-spring-boot-starter"
```

---

## Phase 2：business → gentry-rbac-spring-boot-starter（含 Flyway 迁移）

### Task 3: rbac starter 骨架 + 自动装配 + flyway 依赖

**Files:**
- Rename: `gentry-business/src` → `gentry-rbac-spring-boot-starter/src`（`git mv`）
- Rename: `gentry-business/pom.xml` → `gentry-rbac-spring-boot-starter/pom.xml`（`git mv`，再改内容）
- Modify: `gentry-rbac-spring-boot-starter/pom.xml`：artifactId 改名；`gentry-core` 依赖改新名；**从 gentry-start/pom.xml 迁入 flyway-core + flyway-mysql 依赖**（版本用 `${flyway.version}`，父 pom 已有）；顺手修 `src/test/.../user/mapper/UserMapperXmlTest.java:95` 的兜底路径 `backend/gentry-business/...` → `backend/gentry-rbac-spring-boot-starter/...`
- Modify: `gentry-start/pom.xml`：删 flyway 依赖（已迁 starter）
- Modify: `backend/pom.xml`（modules 与 dependencyManagement：`gentry-business` → `gentry-rbac-spring-boot-starter`）
- Modify: `gentry-start/pom.xml`（依赖坐标）
- Create: `gentry-rbac-spring-boot-starter/src/main/java/com/gentry/rbac/config/GentryRbacAutoConfiguration.java`
- Create: `gentry-rbac-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `gentry-rbac-spring-boot-starter/src/test/java/com/gentry/rbac/config/GentryRbacAutoConfigurationTest.java`

- [ ] **Step 1: git mv + pom 改造**

```bash
cd backend
mkdir -p gentry-rbac-spring-boot-starter
git mv gentry-business/src gentry-rbac-spring-boot-starter/src
git mv gentry-business/pom.xml gentry-rbac-spring-boot-starter/pom.xml
rm -rf gentry-business   # 剩空壳 target/（untracked），整个删掉
rm -rf gentry-rbac-spring-boot-starter/target
```

pom 改动按上方 Files 清单。flyway 依赖块（从 gentry-start 剪切过去）：

```xml
<!-- Flyway：平台迁移（V1–V999）随本 starter jar 分发，消费方免声明 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>${flyway.version}</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>${flyway.version}</version>
</dependency>
```

- [ ] **Step 2: 写失败的自动装配测试（含 enabled=false 不装配）**

```java
package com.gentry.rbac.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GentryRbacAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GentryRbacAutoConfiguration.class));

    @Test
    void imports文件_登记了rbac自动装配类() throws Exception {
        String content = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
        assertThat(content.trim()).isEqualTo("com.gentry.rbac.config.GentryRbacAutoConfiguration");
    }

    @Test
    void 默认条件_装配类参与上下文() {
        // 不加 gentry.rbac 属性 → matchIfMissing=true → 装配类被评估
        // （rbac 全栈 Bean 依赖数据源/Redis，runner 里起不全；这里只锁条件评估本身：
        //  条件通过时上下文会尝试注册装配类，失败原因不含 ConditionalOnProperty skip）
        runner.run(ctx -> assertThat(ctx.getStartupFailure()).isNotNull());
        // 起不来是预期的（缺数据源），但失败必须是「缺基础设施」而不是「条件不匹配」。
        // 精确断言：条件不匹配时上下文能正常启动且无 rbac Bean —— 用下一用例反证。
    }

    @Test
    void rbac关闭时_条件不匹配_上下文干净() {
        runner.withPropertyValues("gentry.rbac.enabled=false")
              .run(ctx -> {
                  assertThat(ctx).hasNotFailed();
                  assertThat(ctx.getBeanNamesForType(GentryRbacAutoConfiguration.class)).isEmpty();
              });
    }
}
```

（`getBeanNamesForType` 断言的是装配类自身未被注册 —— `@ConditionalOnProperty` 挂在类上，条件不过整类跳过。这是「开关真的关得掉」的最小可信验证。）

- [ ] **Step 3: 跑测试确认失败**（`mvn -pl gentry-rbac-spring-boot-starter test -Dtest=GentryRbacAutoConfigurationTest`，编译失败即预期）

- [ ] **Step 4: 实现自动装配类**

```java
package com.gentry.rbac.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * gentry-rbac 自动装配：RBAC + 通知域的组件与 Mapper。
 *
 * <p>{@code gentry.rbac.enabled=false} 时不装配整个域（只引 core 做横切的项目用）。
 * MapperScan 从原 GentryApplication 主类迁移至此；消费方主类不再需要 @MapperScan。</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gentry.rbac", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan({"com.gentry.rbac", "com.gentry.notification"})
@MapperScan({"com.gentry.rbac.**.mapper", "com.gentry.notification.**.mapper"})
public class GentryRbacAutoConfiguration {
}
```

imports 文件内容：`com.gentry.rbac.config.GentryRbacAutoConfiguration`

（`@MapperScan` 在 `@AutoConfiguration` 类上生效：它是 `@Import(MapperScannerRegistrar.class)` 的派生注解，与配置类发现机制无关。已核实。）

- [ ] **Step 5: 全量测试**

```bash
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

预期：全绿。主类 `@MapperScan("com.gentry.**.mapper")` 与自动装配的 MapperScan 并存：MapperFactoryBean 同名（接口 FQN）、同定义类 → Spring 去重，无冲突。

- [ ] **Step 6: Commit**

```bash
git add -A backend
git commit -m "refactor(rbac): gentry-business 改造为 gentry-rbac-spring-boot-starter，flyway 依赖随迁"
```

### Task 4: Flyway 迁移挪入 rbac starter

**Files:**
- Move: `gentry-start/src/main/resources/db/migration/{common,mysql,postgresql,sqlite}` → `gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/{common,mysql,postgresql,sqlite}`（`git mv`，文件内容零改动）
- Modify: `application-mysql.yml`、`application-postgresql.yml`、`application-sqlite.yml`（flyway.locations）
- Modify: `application.yml` 26–28 行的 locations 格式注释（现在写死单流模板，是未来接入方会抄的范本，必须改成双流模板）
- Test: `gentry-start/src/test/java/com/gentry/start/api/FlywayLocationIT.java`（新增）

- [ ] **Step 1: git mv 迁移目录**

```bash
cd backend
mkdir -p gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac
git mv gentry-start/src/main/resources/db/migration/common \
       gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/common
git mv gentry-start/src/main/resources/db/migration/mysql \
       gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/mysql
git mv gentry-start/src/main/resources/db/migration/postgresql \
       gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/postgresql
git mv gentry-start/src/main/resources/db/migration/sqlite \
       gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/sqlite
```

- [ ] **Step 2: 改三个 profile yml 的 flyway.locations + 主 yml 注释**

每个 `application-{mysql,postgresql,sqlite}.yml` 把（mysql 例，其余两个换厂商名）：

```yaml
locations: classpath:db/migration/common,classpath:db/migration/mysql
```

改为：

```yaml
# 双流合并：平台流（rbac-starter jar 内，V1–V999 平台保留段）
#          + 项目流（本模块 classpath，消费项目自 V1000 起）
# 注意保持 classpath: 前缀 —— Flyway 的 ClassPathScanner 走 ClassLoader.getResources()
# 枚举所有 classpath 根（含依赖 jar），天然跨 jar，不要写成 Spring 方言 classpath*:
locations: classpath:db/migration/gentry-rbac/common,classpath:db/migration/gentry-rbac/mysql,classpath:db/migration/common,classpath:db/migration/mysql
```

`gentry-start/src/main/resources/db/migration/` 保留 `common/`、`mysql/` 等空目录 + `.gitkeep`（项目流占位；git 不追踪空目录）。`application.yml` 的 locations 注释块同步改为上述双流说明。

- [ ] **Step 3: 新增 FlywayLocationIT（独立写，不继承 BaseApiIT）**

BaseApiIT 是 `MOCK + @AutoConfigureMockMvc + @Transactional + 每用例真实登录`，本 IT 只需要 ResourcePatternResolver，不要继承（继承会白跑登录且 webEnvironment 冲突）：

```java
package com.gentry.start.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway 双流契约防回归：平台迁移必须能在 starter 的 gentry-rbac/ 目录下被发现
 * （classpath*: 是 Spring 方言，跨 jar 枚举必须带星号），且平台段不越界 V999。
 * 真正执行迁移的是其余 14 个业务 IT（起真实上下文跑 Flyway）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"mysql", "test"})
class FlywayLocationIT {

    @Autowired
    private ResourcePatternResolver resolver;

    @Test
    void 平台流迁移_在starter的gentry_rbac目录下() throws Exception {
        Resource[] all = resolver.getResources("classpath*:db/migration/gentry-rbac/**/V*.sql");
        // common 14 个（V2–V15）+ 三个厂商目录各 1 个 V1 = 17
        assertThat(all.length).isGreaterThanOrEqualTo(17);
    }

    @Test
    void 平台流迁移_版本号不越界V999() throws Exception {
        Resource[] all = resolver.getResources("classpath*:db/migration/gentry-rbac/**/V*.sql");
        int maxVersion = java.util.Arrays.stream(all)
                .map(r -> Integer.parseInt(r.getFilename().replaceFirst("^V", "").replaceAll("__.*$", "")))
                .max(Integer::compareTo).orElse(0);
        assertThat(maxVersion).isLessThan(1000);
    }
}
```

（MOCK 环境与既有 IT 一致，共享上下文缓存，不额外起容器上下文。）

- [ ] **Step 4: 全量测试**

```bash
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

预期：全绿，含 15 个 IT —— Flyway 对已初始化库无感（checksum 只算内容，位置无关）。

- [ ] **Step 5: Commit**

```bash
git add -A backend
git commit -m "refactor(flyway): 迁移挪入 rbac-starter 平台流，版本段 V1-V999 平台保留"
```

---

## Phase 3：主类移包 + monitor-starter + BOM + 文档收口

### Task 5: 主类移包 com.gentry → com.gentry.start

**Files:**
- Move: `gentry-start/src/main/java/com/gentry/GentryApplication.java` → `gentry-start/src/main/java/com/gentry/start/GentryApplication.java`
- Modify: 14 个 IT 若有 `com.gentry.GentryApplication` 显式引用（grep 确认）
- Modify: `gentry-core-spring-boot-starter` 的 `GentryCoreAutoConfiguration`（追加 `@EnableScheduling`，见下）

- [ ] **Step 1: 移包**

```bash
cd backend
mkdir -p gentry-start/src/main/java/com/gentry/start
git mv gentry-start/src/main/java/com/gentry/GentryApplication.java \
       gentry-start/src/main/java/com/gentry/start/GentryApplication.java
```

新内容（**删 `@MapperScan`**——Task 3 已移入 rbac 自动装配；**`@EnableScheduling` 也删**——挪进 core 自动装配，否则消费方不开定时清理就静默失效，`LogCleanTask` 在 rbac starter 里）：

```java
package com.gentry.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GentryApplication {

    public static void main(String[] args) {
        SpringApplication.run(GentryApplication.class, args);
    }
}
```

`GentryCoreAutoConfiguration` 追加：

```java
@AutoConfiguration
@EnableScheduling   // LogCleanTask 等平台 @Scheduled 任务随 starter 生效，消费方免声明
@ComponentScan("com.gentry.core")
public class GentryCoreAutoConfiguration {
}
```

（import `org.springframework.scheduling.annotation.EnableScheduling`。）

**不要加 `scanBasePackages`** —— 自动装配接管平台 Bean；主类包 `com.gentry.start` 只扫狗粮自己的代码（当前狗粮 main 代码只有主类自己）。

- [ ] **Step 2: 修 IT 引用**

```bash
grep -rn "com.gentry.GentryApplication" gentry-start/src --include="*.java"
```

有命中改 `com.gentry.start.GentryApplication`。已核实：`@SpringBootTest` 无 classes 属性时从测试包 `com.gentry.start.api` 向上找 `@SpringBootConfiguration`，第一站 `com.gentry.start` 就命中主类，比现状更近，无风险。

- [ ] **Step 3: 全量测试（本计划最关键的一道验证）**

```bash
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

预期：全绿。这证明没有任何 Bean 再依赖根包扫描，自动装配完整接管。**若挂**：从缺失 Bean 的包名判断漏扫（notification 在 `com.gentry.notification` 已覆盖），补进对应自动装配类的 `@ComponentScan`。

- [ ] **Step 4: Commit**

```bash
git add -A backend
git commit -m "refactor(start): 主类移包 com.gentry.start，EnableScheduling 移入 core 自动装配"
```

### Task 6: gentry-monitor → gentry-monitor-spring-boot-starter + gentry-bom

**Files:**
- Rename: `backend/gentry-monitor/` → `backend/gentry-monitor-spring-boot-starter/`
- Create: `.../src/main/java/com/gentry/monitor/config/GentryMonitorAutoConfiguration.java` + imports
- Test: `.../src/test/java/com/gentry/monitor/config/GentryMonitorAutoConfigurationTest.java`
- Create: `backend/gentry-bom/pom.xml`
- Modify: 父 `pom.xml`（modules 追加 bom + 更名 monitor + dependencyManagement）、`gentry-start/pom.xml`

- [ ] **Step 1: 更名 + pom 改造**（手法同 Task 2，core 依赖用新坐标；`rm -rf` 旧 target）

- [ ] **Step 2: monitor 自动装配类**

```java
package com.gentry.monitor.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * gentry-monitor 自动装配。gentry.monitor.enabled=false 时不装配 Redis 监控域。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "gentry.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("com.gentry.monitor")
public class GentryMonitorAutoConfiguration {
}
```

imports 登记。测试抄 Task 3 的三个用例（imports 断言 + 默认评估 + `gentry.monitor.enabled=false` 时 `getBeanNamesForType(GentryMonitorAutoConfiguration.class)` 为空），路径换 monitor。

- [ ] **Step 3: gentry-bom**

`backend/gentry-bom/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.gentry</groupId>
        <artifactId>gentry-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>gentry-bom</artifactId>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.gentry</groupId>
                <artifactId>gentry-core-spring-boot-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.gentry</groupId>
                <artifactId>gentry-rbac-spring-boot-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.gentry</groupId>
                <artifactId>gentry-monitor-spring-boot-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

父 pom `<modules>` 追加 `<module>gentry-bom</module>`。

- [ ] **Step 4: 全量测试 + install 验证**

```bash
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321! && mvn install -DskipTests
```

预期：全绿 + 全 reactor install 成功（6 模块产物进 ~/.m2）。

- [ ] **Step 5: Commit**

```bash
git add -A backend
git commit -m "feat(monitor,bom): monitor starter 化 + gentry-bom 版本收口"
```

### Task 7: 文档与脚本收口

**Files:**
- Modify: `AGENTS.md`（三章模块表 + 新增「新项目接入」一节 + 七章 Flyway 双流 + 九章如数字变化）
- Modify: `CLAUDE.md`、`DOC_INDEX.md`（收录概要设计）、`README.md`（接入一节）、`doc/guide/RBAC模块开发指南.md`、`doc/guide/Core组件开发指南.md`
- Modify: `scripts/dev_up.sh`（`BACKEND_JAR` 指 gentry-start，模块名未变——确认即可，无需改）

- [ ] **Step 1: AGENTS.md 更新**

三章模块表替换为 5 模块新表（core-starter / rbac-starter / monitor-starter / bom / start），职责列注明「jar 分发，自动装配」；新增小节：

```markdown
### 新项目接入（引依赖，不 fork）

1. 新建 Spring Boot 项目，主类放自己的包（如 com.xxx），不需要任何 @ComponentScan/@MapperScan；
2. `dependencyManagement` 引 `gentry-bom`，依赖 `gentry-rbac-spring-boot-starter`
   （传递 core-starter；flyway-core/flyway-mysql 已随 starter 带，无需自引）；
3. application.yml 最小配置模板：datasource + redis + sa-token（jwt-secret-key 外置）
   + `spring.messages.basename: i18n/messages,i18n/error,i18n/validation,i18n/export`
   + `mybatis-flex.mapper-locations: classpath*:mapper/**/*.xml`
   + `spring.flyway.locations` 双流（模板抄 gentry-start 的 application-mysql.yml）；
4. **版本段保留**：平台迁移 V1–V999（rbac-starter jar 内，勿改勿复制），
   项目迁移自 V1000 起，写在自己项目的 db/migration/{common,...}；
5. 不需要 RBAC/监控时 `gentry.rbac.enabled=false` / `gentry.monitor.enabled=false`。
```

七章补：平台迁移在 starter jar 内 `db/migration/gentry-rbac/`，项目迁移在本项目 `db/migration/`，Flyway 合并成单版本流；**项目侧禁止复制/修改平台段迁移**。

- [ ] **Step 2: 其余文档同步**

全文精确替换旧模块全名（`gentry-core` → `gentry-core-spring-boot-starter`、`gentry-business` → `gentry-rbac-spring-boot-starter`、`gentry-monitor` → `gentry-monitor-spring-boot-starter`），范围：README.md、CLAUDE.md、DOC_INDEX.md、doc/guide/*.md。

**注意：验证残留时不要用 `grep "gentry-core\b"`** —— 连字符是非词字符，`\b` 在 `gentry-core-spring-boot-starter` 的 `e|−` 处恰好构成边界，会成片误报。替换后直接搜旧全名带定界符的形态：

```bash
grep -rn "gentry-business" README.md CLAUDE.md DOC_INDEX.md doc/guide/*.md scripts/*.sh   # 期望零命中
grep -rnE "gentry-core([^-.]|$)|gentry-monitor([^-.]|$)" README.md CLAUDE.md DOC_INDEX.md doc/guide/*.md   # 期望零命中（排除新名前缀）
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "docs: 平台化改造文档收口（AGENTS/README/指南/索引）"
```

### Task 8: 终验 + 「新项目视角」冒烟

- [ ] **Step 1: 后端全量**

```bash
cd backend && mvn clean test -Dspring.datasource.password=CHENLIchenli321!
```

- [ ] **Step 2: E2E（前后端起着）**

```bash
bash scripts/dev_up.sh &    # 或按 README 手动起
cd frontend && npm run test:e2e
```

预期：90 用例全绿。

- [ ] **Step 3: /tmp 冒烟消费项目（终极证明：starter 真的能被外部项目消费）**

目录结构：

```
/tmp/gentry-smoke/
├── pom.xml
└── src/main/
    ├── java/com/demo/DemoApplication.java
    └── resources/
        ├── application.yml
        └── db/migration/common/.gitkeep     # 项目流占位（空也行，Flyway 跳过）
```

`pom.xml` 关键点：parent 用 `spring-boot-starter-parent 3.2.5`；`dependencyManagement` 引 `com.gentry:gentry-bom:1.0.0-SNAPSHOT`（import）；依赖 `gentry-rbac-spring-boot-starter` + `spring-boot-starter-web` + MySQL 驱动。

`DemoApplication.java`：

```java
package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) { SpringApplication.run(DemoApplication.class, args); }
}
```

`application.yml`（**最小完整清单**，缺一项就起不来或登录失败；从 gentry-start 的 application.yml 抽的必需面）：

```yaml
server:
  port: 9099
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/gentry_smoke?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: CHENLIchenli321!
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations: classpath:db/migration/gentry-rbac/common,classpath:db/migration/gentry-rbac/mysql,classpath:db/migration/common
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  messages:
    basename: i18n/messages,i18n/error,i18n/validation,i18n/export
    encoding: UTF-8
    fallback-to-system-locale: false
mybatis-flex:
  mapper-locations: classpath*:mapper/**/*.xml
  global-config:
    print-banner: false
  configuration:
    map-underscore-to-camel-case: true
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 1800
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: jwt-simple
  is-log: false
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET_KEY:gentry-jwt-secret-key-2026-change-me}
gentry:
  i18n:
    enabled: true
    default-locale: zh_CN
    supported-locales: zh_CN,en_US
```

（先 `mysql -uroot -pCHENLIchenli321! -e "CREATE DATABASE gentry_smoke DEFAULT CHARACTER SET utf8mb4"`。）

- [ ] **Step 4: 冒烟验证三件事**

```bash
cd /tmp/gentry-smoke && mvn spring-boot:run &
# 1. 起得来（Flyway 在 gentry_smoke 库建出全部表 = starter 内迁移真的被执行）
mysql -uroot -pCHENLIchenli321! gentry_smoke -e "SHOW TABLES;" | wc -l
# 2. 能登录（RBAC 全栈 Bean 装配成功）
curl -s http://localhost:9099/api/v1/auth/captcha   # 拿验证码（若 captcha.enabled 则走验证码流程）
# 用 chenli / Chenli@2026 走一次真实登录（验证码 math 类型需人算或临时 gentry.captcha.enabled=false 重启）
# 3. 带 Token 拉菜单树返回 RBAC 菜单
```

- [ ] **Step 5: 收尾**

冒烟项目在 /tmp 不入库。若发现 starter 缺配置/缺依赖，**回平台仓库修**（这正是狗粮之外第二消费者的价值），修完重跑 Step 1。全部通过后汇报。

---

## 完成判据（对照概要设计 §6 P1–P3）

1. `mvn clean test` 全绿（基线 355 + 新增自动装配/迁移测试）；
2. `mvn install` 全 reactor 成功，~/.m2 有新坐标产物；
3. /tmp 冒烟项目能起、Flyway 建 表、能登录、菜单树来自 starter；
4. E2E 90 用例全绿；
5. 文档零旧模块名残留（用带定界符的 grep 验证）。
