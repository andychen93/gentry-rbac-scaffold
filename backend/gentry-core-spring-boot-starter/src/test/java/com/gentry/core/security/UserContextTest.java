package com.gentry.core.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserContext 测试。
 *
 * <p>重点是 {@link UserContext#clear()} 必须清掉<b>全部</b>字段——线程池复用下漏清理
 * 会让上一个请求的用户身份/语言泄漏给下一个请求，且只在并发下偶发，极难排查。</p>
 */
class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("setLanguage_读写_一致")
    void setLanguage_读写_一致() {
        assertThat(UserContext.getLanguage()).isNull();
        UserContext.setLanguage("en_US");
        assertThat(UserContext.getLanguage()).isEqualTo("en_US");
    }

    @Test
    @DisplayName("getLanguage_未设置_为null表示跟随浏览器")
    void getLanguage_未设置_为null表示跟随浏览器() {
        // null 是有语义的三态之一，不能用空串代替
        assertThat(UserContext.getLanguage()).isNull();
    }

    @Test
    @DisplayName("clear_调用后_全部字段被清空含language")
    void clear_调用后_全部字段被清空含language() {
        UserContext.setUserId(1L);
        UserContext.setDeptId(3L);
        UserContext.setLanguage("en_US");

        UserContext.clear();

        assertThat(UserContext.getUserId()).isNull();
        assertThat(UserContext.getDeptId()).isNull();
        assertThat(UserContext.getLanguage())
                .as("clear() 漏清 LANGUAGE 会导致线程复用后语言串台")
                .isNull();
    }

    @Test
    @DisplayName("language_线程隔离_互不影响")
    void language_线程隔离_互不影响() throws Exception {
        UserContext.setLanguage("zh_CN");
        String[] other = new String[1];
        Thread t = new Thread(() -> {
            other[0] = UserContext.getLanguage();
            UserContext.setLanguage("en_US");
        });
        t.start();
        t.join();

        assertThat(other[0]).as("新线程不应看到主线程的语言").isNull();
        assertThat(UserContext.getLanguage()).as("主线程不受新线程影响").isEqualTo("zh_CN");
    }
}
