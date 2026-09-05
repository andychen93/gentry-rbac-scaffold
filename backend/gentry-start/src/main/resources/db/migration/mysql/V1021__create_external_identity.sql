-- 外部身份映射表：(identity_provider, external_subject) -> local_user_id
-- 支撑 Keycloak SSO JIT 首次登录同步，见 gentry-oidc-spring-boot-starter 的
-- ExternalIdentityRepository SPI。仅 MySQL 方言（KEY 内联语法非通用），
-- deleted 纳入组合唯一键，因为 MySQL 不支持局部索引。
CREATE TABLE sys_external_identity (
    id                BIGINT       NOT NULL PRIMARY KEY,
    identity_provider VARCHAR(50)  NOT NULL,
    external_subject  VARCHAR(200) NOT NULL,
    local_user_id     BIGINT       NOT NULL,
    extra_attributes  TEXT,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_ext_identity_subject (identity_provider, external_subject, deleted),
    KEY idx_ext_identity_local_user (local_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
