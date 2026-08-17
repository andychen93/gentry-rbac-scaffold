package com.precision.rbac.config;

import com.precision.rbac.config.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 运行时参数解析：<b>sys_config 优先，yml 兜底</b>。
 *
 * <p>此前 {@code sys_config} 表里那几行是死数据 —— 唯一的读入口
 * {@code ConfigService.getConfigValue} 零调用者，参数配置页面改完不影响任何行为，
 * 真正生效的一直是 yml 里的 {@code precision.login-security.*} 等属性。
 * 本类把两者接起来：库里配了就用库里的，没配或格式非法就退回 yml 默认值。</p>
 *
 * <p>读取走 ConfigService 的 Caffeine 缓存，因此不会给每次登录都加一次库查询；
 * 在参数配置页改值或点「刷新缓存」后即时生效。</p>
 *
 * <h3>已接入的键</h3>
 * <table>
 *   <tr><th>sys_config.config_key</th><th>yml 兜底</th></tr>
 *   <tr><td>sys.login.maxFailCount</td><td>precision.login-security.max-fail-count</td></tr>
 *   <tr><td>sys.login.lockMinutes</td><td>precision.login-security.lock-minutes</td></tr>
 *   <tr><td>sys.password.expireDays</td><td>precision.password.expire-days</td></tr>
 *   <tr><td>sys.captcha.enabled</td><td>precision.captcha.enabled</td></tr>
 * </table>
 *
 * <h3>关闭 DB 覆盖</h3>
 * 设 {@code precision.config.db-override=false} 可让本类始终返回 yml 默认值，
 * 完全忽略 sys_config。两个用途：
 * <ul>
 *   <li>集成测试：IT 依赖 application-test.yml 把验证码/锁定阈值/密码过期关掉，
 *       若被库里的值覆盖，测试结果就取决于当前库内容而不再确定
 *       （实测：开启覆盖后所有 *IT 的登录都因 sys.captcha.enabled=true 挂在 20020）。</li>
 *   <li>不可变基础设施：希望配置只由 yml/环境变量掌控、禁止运维在页面上改的部署。</li>
 * </ul>
 */
@Component
public class SysConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(SysConfigResolver.class);

    public static final String KEY_LOGIN_MAX_FAIL_COUNT = "sys.login.maxFailCount";
    public static final String KEY_LOGIN_LOCK_MINUTES = "sys.login.lockMinutes";
    public static final String KEY_PASSWORD_EXPIRE_DAYS = "sys.password.expireDays";
    public static final String KEY_CAPTCHA_ENABLED = "sys.captcha.enabled";

    private final ConfigService configService;

    /** 是否允许 sys_config 覆盖 yml，默认 true */
    private final boolean dbOverride;

    public SysConfigResolver(ConfigService configService,
                             @Value("${precision.config.db-override:true}") boolean dbOverride) {
        this.configService = configService;
        this.dbOverride = dbOverride;
    }

    /**
     * 取整型参数。库中无此键、值为空或不是合法整数时返回 {@code fallback}。
     *
     * <p>解析失败只告警不抛异常：参数配置是运维可改的自由文本，
     * 一个错值不该把登录这种主流程打挂。</p>
     */
    public int getInt(String key, int fallback) {
        String raw = read(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("sys_config[{}]='{}' 不是合法整数，回退到默认值 {}", key, raw, fallback);
            return fallback;
        }
    }

    /**
     * 取布尔参数。只认 true/false（忽略大小写与首尾空格），其余一律回退，
     * 避免把 "1"、"yes" 这类写法误判成 false 而静默关掉验证码之类的安全开关。
     */
    public boolean getBoolean(String key, boolean fallback) {
        String raw = read(key);
        if (raw == null) {
            return fallback;
        }
        String v = raw.trim();
        if ("true".equalsIgnoreCase(v)) {
            return true;
        }
        if ("false".equalsIgnoreCase(v)) {
            return false;
        }
        log.warn("sys_config[{}]='{}' 不是合法布尔值，回退到默认值 {}", key, raw, fallback);
        return fallback;
    }

    private String read(String key) {
        if (!dbOverride) {
            return null;   // 明确关闭 DB 覆盖：一律用 yml 默认值
        }
        try {
            String v = configService.getConfigValue(key);
            return (v == null || v.isBlank()) ? null : v;
        } catch (Exception e) {
            // 读配置失败不能影响主流程（例如库瞬断），退回 yml
            log.warn("读取 sys_config[{}] 失败，回退到默认值: {}", key, e.getMessage());
            return null;
        }
    }
}
