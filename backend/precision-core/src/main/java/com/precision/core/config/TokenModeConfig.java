package com.precision.core.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token Token 模式切换配置。
 *
 * <p>通过配置项 {@code sa-token.token-style} 切换 Token 生成策略：</p>
 * <ul>
 *     <li>{@code uuid}（默认）：UUID Token + Session（保留原有行为，兼容旧版本）</li>
 *     <li>{@code jwt-simple}：JWT Token + Session（Token 为 JWT 格式，Session 仍在 Redis 中维护，支持踢人下线与黑名单）</li>
 * </ul>
 *
 * <p>之所以选择 {@code jwt-simple} 而非 {@code jwt-stateless}，
 * 是因为我们需要保留 Session 能力：在线用户管理、强制下线、角色/权限缓存等。</p>
 */
@Configuration
public class TokenModeConfig {

    /**
     * 注册 Sa-Token JWT Simple 模式的 StpLogic。
     * 仅当 {@code sa-token.token-style=jwt-simple} 时生效。
     */
    @Bean
    @ConditionalOnProperty(name = "sa-token.token-style", havingValue = "jwt-simple")
    public StpLogic stpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
