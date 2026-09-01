package com.gentry.core.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import com.gentry.core.entity.BaseEntity;
import com.gentry.core.interceptor.AutoFillHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置
 * <p>
 * 注册 Insert/Update 监听器。
 *
 * <p>本仓库已拿掉多租户机制（见 {@code doc/design/modules/core/去多租户化-概要设计.md}），
 * 此前在此注册的 {@code TenantManager.setTenantFactory(...)} 与
 * {@code globalConfig.setTenantColumn(...)} 已删除。</p>
 */
@Configuration
public class MybatisFlexConfig {

    @Bean
    public AutoFillHandler autoFillHandler() {
        return new AutoFillHandler();
    }

    @Bean
    public MyBatisFlexCustomizer mybatisFlexCustomizer(AutoFillHandler autoFillHandler) {
        return options -> {
            FlexGlobalConfig globalConfig = FlexGlobalConfig.getDefaultConfig();
            // 注册为全局 InsertListener，对所有继承 BaseEntity 的实体生效
            globalConfig.registerInsertListener(autoFillHandler, BaseEntity.class);
            // 注册为全局 UpdateListener
            globalConfig.registerUpdateListener(autoFillHandler, BaseEntity.class);
        };
    }
}
