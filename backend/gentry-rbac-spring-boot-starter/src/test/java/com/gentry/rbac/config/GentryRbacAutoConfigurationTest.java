package com.gentry.rbac.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配契约测试。全栈行为（Bean 真正起作用）由 gentry-start 的 IT 覆盖，
 * 这里只锁两个契约：imports 登记 + gentry.rbac.enabled 开关有效。
 */
class GentryRbacAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GentryRbacAutoConfiguration.class));

    @Test
    void imports文件_登记了rbac自动装配类() throws Exception {
        // 相对路径读源文件，依赖 surefire 工作目录 = 模块根（gentry-rbac-spring-boot-starter/）这一默认行为。
        // 刻意不走 classpath 读取：classpath 上其他 starter 若带同名 imports 资源，会读错文件。
        String content = Files.readString(Path.of(
                "src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
        assertThat(content.trim()).isEqualTo("com.gentry.rbac.config.GentryRbacAutoConfiguration");
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
