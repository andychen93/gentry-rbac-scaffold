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
