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
