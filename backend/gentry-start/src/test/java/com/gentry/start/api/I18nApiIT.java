package com.gentry.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.mapper.UserMapper;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 国际化端到端集成测试。
 *
 * <p>回归的是这个具体缺陷：改造前 {@code DELETE /api/v1/roles/999999999} 无论带
 * {@code Accept-Language: en-US} 还是 {@code zh-CN} 都返回「角色不存在」——
 * 因为 {@code ErrorCode} 的中文写死在枚举里、{@code GlobalExceptionHandler} 直接透传。</p>
 *
 * <p>同时覆盖语言解析三级链的优先级、{@code Accept-Language} 的两级匹配，以及
 * <b>{@code spring.messages.basename} 的装配</b>——第一次跑这套用例时全部失败，
 * 根因就是资源文件建好了但 basename 里漏登记 {@code i18n/error}，
 * 症状是「文件存在、测试读得到，但 MessageSource 找不到，全量走中文兜底」。</p>
 */
class I18nApiIT extends BaseApiIT {

    /** 不存在的角色 ID，用于稳定触发 ROLE_NOT_FOUND(20015) */
    private static final long ABSENT_ROLE_ID = 999_999_999L;

    /** 内置角色 ADMIN，删除时触发 error.role.builtin.undeletable */
    private static final long BUILTIN_ROLE_ID = 1L;

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String TEST_PWD = "I18n@1234";

    /**
     * 用 MyBatis 而不是 {@code JdbcTemplate} 改库。
     *
     * <p>踩过的坑：{@code JdbcTemplate} 注入的是原始 {@code DataSource}，而 MyBatis-Flex
     * 用 {@code FlexDataSource} 包装后才绑定到 {@code @Transactional}——两者不是同一条物理
     * 连接。于是 JdbcTemplate 的写操作自成一个事务，去撞测试事务持有的行锁，
     * 报 {@code Lock wait timeout exceeded} 并白等 50 秒。走 Mapper 才在同一连接里。</p>
     */
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageSource messageSource;

    // ==================== 辅助 ====================

    private JsonNode deleteAbsentRole(String acceptLanguage, String authHeader) throws Exception {
        MvcResult result = mockMvc.perform(
                        bareDelete("/api/v1/roles/{id}", ABSENT_ROLE_ID)
                                .header("Authorization", authHeader)
                                .header("Accept-Language", acceptLanguage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20015))   // 码不变，只有 message 本地化
                .andReturn();
        return parse(result);
    }

    /**
     * 建一个带 ADMIN 角色的新用户、设定其 {@code language}、再登录，返回其 Authorization 头。
     *
     * <p><b>为什么不直接改种子用户（如 zhangsan）：</b>本类带 {@code @Transactional}，
     * 改动随用例回滚；但用新建用户能顺带保证「其他用例看到的种子数据没被动过」，
     * 也避免 {@code WHERE username = ?} 这种走不到索引（唯一键最左列是 tenant_id）
     * 而全表加锁的写法。这里一律按主键更新。</p>
     *
     * @param language 传 null 表示不设置（即 sys_user.language IS NULL，三态里的「未选过」）
     */
    private String loginWithLanguage(String language) throws Exception {
        String username = "i18n" + SEQ.incrementAndGet();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("nickname", "国际化测试用户");
        body.put("password", TEST_PWD);
        body.put("deptId", 100);            // buildLoginResult 里 deptId 为 null 会 NPE
        body.put("status", 1);
        body.put("roleIds", List.of(BUILTIN_ROLE_ID));   // ADMIN，需要 system:role:delete
        MvcResult created = mockMvc.perform(authedPost("/api/v1/users").content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        if (language != null) {
            User patch = new User();
            patch.setId(parse(created).at("/data/id").asLong());   // 按主键更新，只锁一行
            patch.setLanguage(language);
            int n = userMapper.update(patch);                      // null 字段不参与 SET
            assertThat(n).as("应更新到刚建的用户").isEqualTo(1);
        }
        return login(username, TEST_PWD);
    }

    // ==================== MessageSource 装配 ====================

    @Test
    @DisplayName("MessageSource能取到error资源包_验证basename已登记")
    void MessageSource能取到error资源包_验证basename已登记() {
        // 这条直接盯住 spring.messages.basename 的装配。资源文件存在 ≠ MessageSource 能加载，
        // 漏登记 basename 的症状是全量走 defaultValue（显示中文），不报任何错。
        assertThat(messageSource.getMessage("error.role.not.found", null, "MISS", Locale.US))
                .isEqualTo("Role not found");
        assertThat(messageSource.getMessage("error.role.not.found", null, "MISS", Locale.CHINA))
                .isEqualTo("角色不存在");
        // 显式 messageKey 那批也必须在同一个 basename 下可见
        assertThat(messageSource.getMessage("error.role.builtin.undeletable", null, "MISS", Locale.US))
                .isEqualTo("Built-in role cannot be deleted");
        // 既有的 messages 资源包不能被新 basename 挤掉
        assertThat(messageSource.getMessage("operlog.type.INSERT", null, "MISS", Locale.US))
                .isEqualTo("Add");
    }

    // ==================== ② 请求头（用户未设置偏好） ====================

    @Test
    @DisplayName("删除不存在的角色_请求头enUS_返回英文消息")
    void 删除不存在的角色_请求头enUS_返回英文消息() throws Exception {
        assertThat(deleteAbsentRole("en-US", auth).at("/message").asText())
                .as("改造前这里返回的是「角色不存在」——i18n 是摆设的原始证据")
                .isEqualTo("Role not found");
    }

    @Test
    @DisplayName("删除不存在的角色_请求头zhCN_返回中文消息")
    void 删除不存在的角色_请求头zhCN_返回中文消息() throws Exception {
        assertThat(deleteAbsentRole("zh-CN", auth).at("/message").asText()).isEqualTo("角色不存在");
    }

    @Test
    @DisplayName("请求头只有语言码无地区_退化匹配到enUS")
    void 请求头只有语言码无地区_退化匹配到enUS() throws Exception {
        // 浏览器可能只发 "en"，只做精确匹配会落空
        assertThat(deleteAbsentRole("en", auth).at("/message").asText()).isEqualTo("Role not found");
    }

    @Test
    @DisplayName("请求头zhTW_退化命中zhCN而非英文")
    void 请求头zhTW_退化命中zhCN而非英文() throws Exception {
        // 有意为之：繁体用户看简体优于看英文
        assertThat(deleteAbsentRole("zh-TW", auth).at("/message").asText()).isEqualTo("角色不存在");
    }

    @Test
    @DisplayName("请求头为不支持语言_退默认中文")
    void 请求头为不支持语言_退默认中文() throws Exception {
        assertThat(deleteAbsentRole("ja-JP", auth).at("/message").asText()).isEqualTo("角色不存在");
    }

    @Test
    @DisplayName("请求头畸形_不报错并退默认中文")
    void 请求头畸形_不报错并退默认中文() throws Exception {
        assertThat(deleteAbsentRole("=====;;q=abc", auth).at("/message").asText()).isEqualTo("角色不存在");
    }

    // ==================== ① 用户偏好优先于请求头 ====================

    @Test
    @DisplayName("用户偏好enUS_即使请求头是zhCN也返回英文")
    void 用户偏好enUS_即使请求头是zhCN也返回英文() throws Exception {
        String userAuth = loginWithLanguage("en_US");
        assertThat(deleteAbsentRole("zh-CN", userAuth).at("/message").asText())
                .as("① sys_user.language 必须压过 ② Accept-Language")
                .isEqualTo("Role not found");
    }

    @Test
    @DisplayName("用户偏好为NULL_跟随请求头_三态语义生效")
    void 用户偏好为NULL_跟随请求头_三态语义生效() throws Exception {
        String userAuth = loginWithLanguage(null);   // 未设置 = 从未选过
        assertThat(deleteAbsentRole("en-US", userAuth).at("/message").asText())
                .as("language 为 NULL 时必须跟随浏览器，这是 V12 选可空列的理由")
                .isEqualTo("Role not found");
    }

    @Test
    @DisplayName("用户偏好为不支持语言_降级到请求头")
    void 用户偏好为不支持语言_降级到请求头() throws Exception {
        String userAuth = loginWithLanguage("ja_JP");
        assertThat(deleteAbsentRole("en-US", userAuth).at("/message").asText())
                .as("白名单外的值必须被忽略，而不是让整站退回 key")
                .isEqualTo("Role not found");
    }

    @Test
    @DisplayName("种子用户的language为NULL_V12不填默认值")
    void 种子用户的language为NULL_V12不填默认值() {
        // V12 迁移刻意不填默认值：NOT NULL DEFAULT 'zh_CN' 会让三级链的第 ② 级永不执行
        for (String seed : new String[]{"chenli", "admin", "zhangsan"}) {
            User u = userMapper.selectByUsername(1L, seed);
            assertThat(u).as("种子用户 %s 应存在", seed).isNotNull();
            assertThat(u.getLanguage()).as("种子用户 %s 的 language 应为 NULL", seed).isNull();
        }
    }

    // ==================== 显式 messageKey ====================

    @Test
    @DisplayName("显式messageKey的业务提示_按语言本地化")
    void 显式messageKey的业务提示_按语言本地化() throws Exception {
        // 「系统内置角色不可删除」走 BizException(PARAM_ERROR, "error.role.builtin.undeletable")
        MvcResult en = mockMvc.perform(bareDelete("/api/v1/roles/{id}", BUILTIN_ROLE_ID)
                        .header("Authorization", auth)
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(en).at("/message").asText()).isEqualTo("Built-in role cannot be deleted");

        MvcResult zh = mockMvc.perform(bareDelete("/api/v1/roles/{id}", BUILTIN_ROLE_ID)
                        .header("Authorization", auth)
                        .header("Accept-Language", "zh-CN"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(zh).at("/message").asText()).isEqualTo("系统内置角色不可删除");
    }

    @Test
    @DisplayName("带参messageKey_占位符被正确替换")
    void 带参messageKey_占位符被正确替换() throws Exception {
        // 建个用户，再给它分配不存在的角色 → error.role.not.found.detail = "Role not found: {0}"
        Map<String, Object> body = new HashMap<>();
        String username = "i18n" + SEQ.incrementAndGet();
        body.put("username", username);
        body.put("nickname", "占位符测试");
        body.put("password", TEST_PWD);
        body.put("deptId", 100);
        body.put("status", 1);
        MvcResult created = mockMvc.perform(authedPost("/api/v1/users").content(json(body)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long userId = parse(created).at("/data/id").asLong();

        MvcResult result = mockMvc.perform(authedPut("/api/v1/users/{id}/roles", userId)
                        .header("Accept-Language", "en-US")
                        .content(json(Map.of("roleIds", List.of(ABSENT_ROLE_ID)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20015))
                .andReturn();

        assertThat(parse(result).at("/message").asText())
                .as("占位符必须被替换；出现裸 {0} 说明 args 没传或被 MessageFormat 单引号吞掉")
                .doesNotContain("{0}")
                .startsWith("Role not found:")
                .contains(String.valueOf(ABSENT_ROLE_ID));
    }
}
