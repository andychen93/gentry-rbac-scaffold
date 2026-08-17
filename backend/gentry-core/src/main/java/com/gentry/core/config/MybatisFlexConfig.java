package com.gentry.core.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.tenant.TenantManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import com.gentry.core.entity.BaseEntity;
import com.gentry.core.entity.TenantEntity;
import com.gentry.core.interceptor.AutoFillHandler;
import com.gentry.core.tenant.GentryTenantManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置
 * <p>
 * 注册 Insert/Update 监听器、多租户管理器。
 */
@Configuration
public class MybatisFlexConfig {

    @Bean
    public AutoFillHandler autoFillHandler() {
        return new AutoFillHandler();
    }

    @Bean
    public GentryTenantManager gentryTenantManager() {
        return new GentryTenantManager();
    }

    @Bean
    public MyBatisFlexCustomizer mybatisFlexCustomizer(AutoFillHandler autoFillHandler,
                                                       GentryTenantManager gentryTenantManager) {
        return options -> {
            FlexGlobalConfig globalConfig = FlexGlobalConfig.getDefaultConfig();
            // 注册为全局 InsertListener，对所有继承 BaseEntity/TenantEntity 的实体生效
            globalConfig.registerInsertListener(autoFillHandler, BaseEntity.class, TenantEntity.class);
            // 注册为全局 UpdateListener
            globalConfig.registerUpdateListener(autoFillHandler, BaseEntity.class, TenantEntity.class);
            // 注册多租户 Factory
            TenantManager.setTenantFactory(gentryTenantManager);
            // 设置全局租户字段名
            globalConfig.setTenantColumn("tenant_id");
        };
    }
}
