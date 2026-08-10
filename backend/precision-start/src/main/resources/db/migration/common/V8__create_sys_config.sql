-- V8: 系统参数配置表（平台级全局表，三库通用类型；不含 tenant_id）
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT       NOT NULL PRIMARY KEY,
    config_name  VARCHAR(100),
    config_key   VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    config_type  VARCHAR(1)   DEFAULT 'N',   -- Y=系统内置 N=业务自定义
    remark       VARCHAR(500),
    create_by    BIGINT,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

-- 预置业务参数（供 AuthService 等读取，优先级 sys_config > yml）
INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, remark) VALUES
    (1, '登录失败最大次数',     'sys.login.maxFailCount', '5',    'Y', '连续登录失败N次后锁定账号'),
    (2, '账号锁定时长(分钟)',   'sys.login.lockMinutes',  '10',   'Y', '锁定时长即失败计数 Redis TTL'),
    (3, '密码过期天数',         'sys.password.expireDays','90',   'Y', '0或负数表示不校验'),
    (4, '验证码开关',           'sys.captcha.enabled',    'true', 'Y', '是否启用登录验证码');
