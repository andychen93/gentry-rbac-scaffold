package com.precision.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志保留策略配置。
 *
 * <pre>
 * precision:
 *   log:
 *     keep-days: 90
 * </pre>
 *
 * <p>由日志定时清理任务（LogCleanTask）读取，清理 sys_oper_log / sys_login_log
 * 超出 {@link #keepDays} 的记录。</p>
 */
@Component
@ConfigurationProperties(prefix = "precision.log")
public class LogProperties {

    /** 操作/登录日志保留天数，定时任务清理超出部分 */
    private int keepDays = 90;

    public int getKeepDays() { return keepDays; }
    public void setKeepDays(int keepDays) { this.keepDays = keepDays; }
}
