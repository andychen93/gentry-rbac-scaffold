package com.gentry.monitor.redis.constant;

import com.gentry.monitor.redis.vo.RedisKeyDefineVO;

import java.util.List;

/**
 * 系统中使用的 Redis Key 模板定义，供监控页面展示与过滤。
 *
 * <p>注意：Sa-Token 的 key 前缀来源于配置 {@code sa-token.token-name}（当前为 {@code Authorization}），
 * 此处以当前部署为准。如修改 {@code token-name}，同步更新此处定义。</p>
 */
public final class RedisKeyDefines {

    private RedisKeyDefines() {}

    private static final List<RedisKeyDefineVO> DEFINES = List.of(
            new RedisKeyDefineVO(
                    "token_mapping",
                    "Authorization:login:token:*",
                    "Token → LoginId 映射（Sa-Token）",
                    1800L),
            new RedisKeyDefineVO(
                    "user_session",
                    "Authorization:login:session:*",
                    "用户 Session（Sa-Token，含 userId/roles 等）",
                    1800L),
            new RedisKeyDefineVO(
                    "jwt_blacklist",
                    "blacklist:*",
                    "JWT Token 黑名单（登出/改密码/强制下线）",
                    -2L),
            new RedisKeyDefineVO(
                    "token_timeout",
                    "Authorization:timeout:*",
                    "Token 过期时间辅助 Key（Sa-Token）",
                    1800L),
            new RedisKeyDefineVO(
                    "dict_cache",
                    "dict:*",
                    "字典数据二级缓存 L2（L1 为各节点本地 Caffeine，失效经 dict:invalidate 频道广播）",
                    1800L)
    );

    public static List<RedisKeyDefineVO> getAll() {
        return DEFINES;
    }
}
