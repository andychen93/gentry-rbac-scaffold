-- V16: 邮箱认证令牌表（注册验证 / 密码找回共用，三库通用写法）
--   1. 一次性令牌只存 SHA-256（token_hash），不存明文；唯一入口 uk_token_hash。
--   2. purpose 区分用途：REGISTER（验证通过时创建用户）/ RESET_PASSWORD。
--   3. payload 暂存注册凭据（BCrypt 密码哈希 + 昵称，JSON）：注册先发信、验证才建用户，
--      密码必须在令牌里等验证。令牌一次性，用后置 deleted=1，残留窗口极短。
--   4. TTL 由服务层按 purpose 控制（REGISTER 48h / RESET 30min），expires_at 落库。
--   5. email 统一小写规范化后存储；idx_email_purpose 供重发冷却与邮箱占用查询。
--   6. 索引不用 IF NOT EXISTS、唯一约束用 CREATE UNIQUE INDEX（MySQL 的 CREATE INDEX
--      不支持 IF NOT EXISTS，内联 UNIQUE KEY 又是 MySQL 方言——对齐 V11 的做法）。
CREATE TABLE IF NOT EXISTS sys_email_token (
    id          BIGINT       NOT NULL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    email       VARCHAR(255) NOT NULL,
    purpose     VARCHAR(20)  NOT NULL,              -- REGISTER / RESET_PASSWORD
    token_hash  CHAR(64)     NOT NULL,              -- 令牌 SHA-256，不存明文
    payload     VARCHAR(500) NULL,                  -- 注册凭据 JSON（bcrypt哈希+昵称）
    user_id     BIGINT       NULL,                  -- REGISTER 验证通过后回填
    expires_at  TIMESTAMP    NOT NULL,
    used_at     TIMESTAMP    NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_email_token_hash ON sys_email_token (token_hash);
CREATE INDEX idx_email_token_purpose ON sys_email_token (tenant_id, email, purpose);
