package com.gentry.rbac.log.task;

import com.gentry.core.config.LogProperties;
import com.gentry.rbac.log.mapper.LoginLogMapper;
import com.gentry.rbac.log.mapper.OperLogMapper;
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
 * <p>保留天数由 {@code gentry.log.keep-days} 配置（见 LogProperties）。</p>
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
        int oper = operLogMapper.deleteBeforeTime(cutoff);
        int login = loginLogMapper.deleteBeforeTime(cutoff);
        log.info("Log cleanup done: kept {} days, deleted {} oper logs and {} login logs", keepDays, oper, login);
    }
}
