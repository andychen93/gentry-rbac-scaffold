package com.precision.core.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;

import java.util.List;

/**
 * JWT Token 黑名单服务。
 *
 * <p>在 JWT 模式下，Token 自带签名校验（本地验签即可），为了实现"登出即失效"、"改密码踢设备"等需求，
 * 引入 Redis 黑名单机制：被吊销的 Token 加入黑名单，拦截器在每次请求时检查。</p>
 *
 * <p>Redis Key 规范：</p>
 * <pre>
 *   Key:   blacklist:{tokenValue}
 *   Value: "1"
 *   TTL:   Token 剩余有效期（秒），过期自动清理
 * </pre>
 */
public final class TokenBlacklistService {

    /** Redis Key 前缀 */
    static final String BLACKLIST_PREFIX = "blacklist:";

    private TokenBlacklistService() {
    }

    /**
     * 将指定 Token 加入黑名单。
     * TTL 取 Token 当前剩余有效期，过期后自动清理，避免黑名单无限增长。
     *
     * @param tokenValue Token 值（不含 Bearer 前缀）
     */
    public static void addToBlacklist(String tokenValue) {
        if (tokenValue == null || tokenValue.isEmpty()) {
            return;
        }
        long ttl = StpUtil.stpLogic.getTokenTimeout(tokenValue);
        // ttl 说明：>0 正常剩余秒数；-1 永不过期；-2 不存在/已过期；0 已过期
        if (ttl == 0 || ttl == -2) {
            return;
        }
        if (ttl == -1) {
            // 永不过期 Token 兜底：按 sa-token 配置 timeout 记录
            ttl = SaManager.getConfig().getTimeout();
            if (ttl <= 0) {
                ttl = 7 * 24 * 3600L;
            }
        }
        SaManager.getSaTokenDao().set(BLACKLIST_PREFIX + tokenValue, "1", ttl);
    }

    /**
     * 将某用户所有有效 Token 加入黑名单。
     * 常用于改密码、封禁、管理员强制下线等场景。
     *
     * @param loginId 用户标识
     */
    public static void blacklistAllTokensOfUser(Object loginId) {
        if (loginId == null) {
            return;
        }
        List<String> tokens = StpUtil.stpLogic.getTokenValueListByLoginId(loginId);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        for (String token : tokens) {
            addToBlacklist(token);
        }
    }

    /**
     * 判断 Token 是否在黑名单。
     *
     * @param tokenValue Token 值
     * @return true 表示已被吊销
     */
    public static boolean isBlacklisted(String tokenValue) {
        if (tokenValue == null || tokenValue.isEmpty()) {
            return false;
        }
        return SaManager.getSaTokenDao().get(BLACKLIST_PREFIX + tokenValue) != null;
    }
}
