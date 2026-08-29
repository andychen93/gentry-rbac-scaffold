package com.gentry.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配契约测试。全栈行为（Bean 真正起作用）由 gentry-start 的 14 个 IT 覆盖，
 * 这里只锁三个静态契约：imports 登记 + 扫描包正确 + 先于 WebMvc 自动配置。
 */
class GentryCoreAutoConfigurationTest {

    @Test
    void imports文件_登记了core自动装配类() throws Exception {
        // 相对路径读源文件，依赖 surefire 工作目录 = 模块根（gentry-core-spring-boot-starter/）这一默认行为。
        // 刻意不走 classpath 读取：classpath 上其他 starter 若带同名 imports 资源，会读错文件。
        String content = Files.readString(Path.of(
                "src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"));
        assertThat(content.trim()).isEqualTo("com.gentry.core.config.GentryCoreAutoConfiguration");
    }

    @Test
    void 注解_扫描com_gentry_core包() {
        ComponentScan scan = GentryCoreAutoConfiguration.class.getAnnotation(ComponentScan.class);
        assertThat(scan).isNotNull();
        assertThat(scan.value()).containsExactly("com.gentry.core");
    }

    @Test
    void 注解_先于WebMvc自动配置注册() {
        // I18nConfig 的 localeResolver 与 Boot WebMvc 的同名条件 Bean 竞争，
        // 必须保持 core 在前、Boot 侧 @ConditionalOnMissingBean 让路；
        // 误删该顺序会回归成 Boot 的 AcceptHeaderLocaleResolver（IT 有兜底，这里显式锁契约）。
        AutoConfigureBefore before = GentryCoreAutoConfiguration.class.getAnnotation(AutoConfigureBefore.class);
        assertThat(before).isNotNull();
        assertThat(before.value()).containsExactly(WebMvcAutoConfiguration.class);
    }
}
