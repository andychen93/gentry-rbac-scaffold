package com.gentry.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务调度配置。
 *
 * <p>提供独立的 {@link ThreadPoolTaskScheduler}（Bean 名 taskScheduler），让 @Scheduled
 * 任务跑在线程池而非默认单线程，避免日志清理等任务相互阻塞。</p>
 *
 * <p>需配合 {@link org.springframework.scheduling.annotation.EnableScheduling}（已在
 * {@link GentryCoreAutoConfiguration} 开启，随 starter 对消费方生效）。</p>
 *
 * <p>注意：@Scheduled 任务跑在调度线程，没有 UserContext（ThreadLocal 为空），
 * 任务内如需跨租户操作须显式 {@code @IgnoreTenant} 或
 * {@code TenantManager.ignoreTenantCondition()}。</p>
 */
@Configuration
public class SchedulingConfig {

    @Bean(name = "taskScheduler", destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("gentry-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
