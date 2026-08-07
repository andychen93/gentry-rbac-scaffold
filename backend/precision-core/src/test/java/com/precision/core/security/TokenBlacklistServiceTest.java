package com.precision.core.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TokenBlacklistService 单元测试。
 * <p>使用内存 {@link SaTokenDao} 与定制 {@link StpLogic} 模拟 Sa-Token 环境，无需启动 Spring 或 Redis。</p>
 */
@DisplayName("Token 黑名单服务测试")
class TokenBlacklistServiceTest {

    private SaTokenDao originalDao;
    private StpLogic originalStpLogic;
    private MemorySaTokenDao memDao;
    private FakeStpLogic fakeLogic;

    @BeforeEach
    void setup() {
        originalDao = SaManager.getSaTokenDao();
        originalStpLogic = StpUtil.stpLogic;
        memDao = new MemorySaTokenDao();
        SaManager.setSaTokenDao(memDao);
        fakeLogic = new FakeStpLogic();
        StpUtil.setStpLogic(fakeLogic);
        // 确保配置存在
        SaConfigEnsurer.ensure();
    }

    @AfterEach
    void tearDown() {
        SaManager.setSaTokenDao(originalDao);
        StpUtil.setStpLogic(originalStpLogic);
    }

    @Test
    @DisplayName("空 token 不入黑名单")
    void addToBlacklist_nullOrEmpty_noop() {
        TokenBlacklistService.addToBlacklist(null);
        TokenBlacklistService.addToBlacklist("");
        assertThat(memDao.store).isEmpty();
    }

    @Test
    @DisplayName("有效 token 加入黑名单，key 格式正确，TTL 等于 token 剩余时长")
    void addToBlacklist_validToken_storedWithTtl() {
        fakeLogic.tokenTimeouts.put("tok-A", 1200L);

        TokenBlacklistService.addToBlacklist("tok-A");

        String key = TokenBlacklistService.BLACKLIST_PREFIX + "tok-A";
        assertThat(memDao.store).containsKey(key);
        assertThat(memDao.store.get(key)).isEqualTo("1");
        assertThat(memDao.timeouts.get(key)).isEqualTo(1200L);
    }

    @Test
    @DisplayName("已过期 token（ttl=-2）不写入黑名单")
    void addToBlacklist_expired_skip() {
        fakeLogic.tokenTimeouts.put("tok-expired", -2L);
        TokenBlacklistService.addToBlacklist("tok-expired");
        assertThat(memDao.store).isEmpty();
    }

    @Test
    @DisplayName("永不过期 token（ttl=-1）使用 sa-token timeout 兜底")
    void addToBlacklist_neverExpire_fallbackTimeout() {
        fakeLogic.tokenTimeouts.put("tok-forever", -1L);
        TokenBlacklistService.addToBlacklist("tok-forever");
        String key = TokenBlacklistService.BLACKLIST_PREFIX + "tok-forever";
        assertThat(memDao.store).containsKey(key);
        assertThat(memDao.timeouts.get(key)).isPositive();
    }

    @Test
    @DisplayName("批量拉黑用户所有 token")
    void blacklistAllTokensOfUser_multipleTokens_allBlacklisted() {
        fakeLogic.userTokens.put("100", Arrays.asList("tok-1", "tok-2", "tok-3"));
        fakeLogic.tokenTimeouts.put("tok-1", 500L);
        fakeLogic.tokenTimeouts.put("tok-2", 600L);
        fakeLogic.tokenTimeouts.put("tok-3", 700L);

        TokenBlacklistService.blacklistAllTokensOfUser(100L);

        assertThat(memDao.store).containsKeys(
                TokenBlacklistService.BLACKLIST_PREFIX + "tok-1",
                TokenBlacklistService.BLACKLIST_PREFIX + "tok-2",
                TokenBlacklistService.BLACKLIST_PREFIX + "tok-3"
        );
    }

    @Test
    @DisplayName("用户无 token 时 batch 方法不抛异常")
    void blacklistAllTokensOfUser_noTokens_noop() {
        fakeLogic.userTokens.put("999", Collections.emptyList());
        TokenBlacklistService.blacklistAllTokensOfUser(999L);
        assertThat(memDao.store).isEmpty();
    }

    @Test
    @DisplayName("loginId 为 null 时 batch 方法静默返回")
    void blacklistAllTokensOfUser_nullLoginId_noop() {
        TokenBlacklistService.blacklistAllTokensOfUser(null);
        assertThat(memDao.store).isEmpty();
    }

    @Test
    @DisplayName("isBlacklisted 返回正确状态")
    void isBlacklisted_checkStatus() {
        fakeLogic.tokenTimeouts.put("tok-X", 300L);
        TokenBlacklistService.addToBlacklist("tok-X");

        assertThat(TokenBlacklistService.isBlacklisted("tok-X")).isTrue();
        assertThat(TokenBlacklistService.isBlacklisted("tok-not-exist")).isFalse();
        assertThat(TokenBlacklistService.isBlacklisted(null)).isFalse();
        assertThat(TokenBlacklistService.isBlacklisted("")).isFalse();
    }

    // ============ 辅助类 ============

    /** 保证 SaManager 有基本配置 */
    private static class SaConfigEnsurer {
        static void ensure() {
            if (SaManager.getConfig() == null) {
                SaTokenConfig cfg = new SaTokenConfig();
                cfg.setTimeout(1800);
                SaManager.setConfig(cfg);
            } else {
                SaManager.getConfig().setTimeout(1800);
            }
        }
    }

    /** 内存版 SaTokenDao */
    static class MemorySaTokenDao implements SaTokenDao {
        final Map<String, String> store = new HashMap<>();
        final Map<String, Long> timeouts = new HashMap<>();
        final Map<String, Object> objStore = new HashMap<>();
        final Map<String, Long> objTimeouts = new HashMap<>();

        @Override public String get(String key) { return store.get(key); }
        @Override public void set(String key, String value, long timeout) {
            store.put(key, value); timeouts.put(key, timeout);
        }
        @Override public void update(String key, String value) { store.put(key, value); }
        @Override public void delete(String key) { store.remove(key); timeouts.remove(key); }
        @Override public long getTimeout(String key) { return timeouts.getOrDefault(key, -2L); }
        @Override public void updateTimeout(String key, long timeout) { timeouts.put(key, timeout); }

        @Override public Object getObject(String key) { return objStore.get(key); }
        @Override public void setObject(String key, Object object, long timeout) {
            objStore.put(key, object); objTimeouts.put(key, timeout);
        }
        @Override public void updateObject(String key, Object object) { objStore.put(key, object); }
        @Override public void deleteObject(String key) { objStore.remove(key); objTimeouts.remove(key); }
        @Override public long getObjectTimeout(String key) { return objTimeouts.getOrDefault(key, -2L); }
        @Override public void updateObjectTimeout(String key, long timeout) { objTimeouts.put(key, timeout); }
        @Override public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
            return store.keySet().stream()
                    .filter(k -> k.startsWith(prefix) && (keyword == null || keyword.isEmpty() || k.contains(keyword)))
                    .skip(start)
                    .limit(size <= 0 ? Long.MAX_VALUE : size)
                    .toList();
        }
    }

    /** 定制 StpLogic，仅实现测试用到的方法 */
    static class FakeStpLogic extends StpLogic {
        final Map<String, Long> tokenTimeouts = new HashMap<>();
        final Map<String, List<String>> userTokens = new HashMap<>();

        FakeStpLogic() { super("login"); }

        @Override
        public long getTokenTimeout(String tokenValue) {
            return tokenTimeouts.getOrDefault(tokenValue, -2L);
        }

        @Override
        public List<String> getTokenValueListByLoginId(Object loginId) {
            return userTokens.getOrDefault(String.valueOf(loginId), Collections.emptyList());
        }
    }
}
