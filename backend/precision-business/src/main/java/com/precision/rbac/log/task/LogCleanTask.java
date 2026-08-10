package com.precision.rbac.log.task;

import com.mybatisflex.core.tenant.TenantManager;
import com.precision.core.config.LogProperties;
import com.precision.rbac.log.mapper.LoginLogMapper;
import com.precision.rbac.log.mapper.OperLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 日志定时清理任务。
 *
 * <p>每天凌晨 2:30 清理 sys_oper_log / sys_login_log 中超出保留期的记录。</p>
 *
 * <p><b>跨租户</b>：@Scheduled 跑在调度线程，无 UserContext（ThreadLocal 为空），
 * 故用 {@link TenantManager#ignoreTenantCondition()} 临时跳过租户拦截，
 * 配合 Mapper 的 cleanExpiredBefore（SQL 不带 tenant_id）清理所有租户数据。</p>
 *
 * <p>保留天数由 {@code precision.log.keep-days} 配置（见 LogProperties）。</p>
 */
@Component
public class LogCleanTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanTask.class);

    private final OperLogMapper operLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final LogProperties logProperties;

    public LogCleanTask(OperLogMapper operLogMapper, LoginLogMapper loginLogMapper, LogProperties logProperties) {
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.logProperties = logProperties;
    }

    /** 每天凌晨 2:30 执行 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanExpiredLogs() {
        int keepDays = logProperties.getKeepDays();
        if (keepDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        try {
            TenantManager.ignoreTenantCondition();
            int oper = operLogMapper.cleanExpiredBefore(cutoff);
            int login = loginLogMapper.cleanExpiredBefore(cutoff);
            log.info("日志清理完成：保留 {} 天，删除操作日志 {} 条、登录日志 {} 条", keepDays, oper, login);
        } finally {
            TenantManager.restoreTenantCondition();
        }
    }
}
