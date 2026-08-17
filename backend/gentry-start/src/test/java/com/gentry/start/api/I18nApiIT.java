package com.gentry.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.mapper.UserMapper;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
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

    // ==================== B 类 key 下发 ====================

    /** 递归收集菜单树上的 (name, i18nKey) */
    private void collectMenus(JsonNode nodes, List<String[]> out) {
        for (JsonNode n : nodes) {
            out.add(new String[]{n.path("name").asText(), n.path("i18nKey").asText(null)});
            if (n.has("children") && n.get("children").isArray()) {
                collectMenus(n.get("children"), out);
            }
        }
    }

    @Test
    @DisplayName("登录响应带language与菜单树i18nKey")
    void 登录响应带language与菜单树i18nKey() throws Exception {
        MvcResult result = mockMvc.perform(bareGet("/api/v1/auth/user-info")
                        .header("Authorization", superAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = parse(result).at("/data");

        // 全局 Jackson 配了 NON_NULL，language 为 null（种子用户从未选过）时字段整个不出现。
        // 语义上等价：缺失 = null = 未设置 = 前端保持当前 locale。
        assertThat(data.has("language"))
                .as("种子用户 language 为 NULL，NON_NULL 下该字段应被省略")
                .isFalse();

        List<String[]> menus = new ArrayList<>();
        collectMenus(data.get("menus"), menus);
        assertThat(menus).as("SUPER_ADMIN 应能看到导航菜单").isNotEmpty();

        List<String> missing = menus.stream()
                .filter(m -> m[1] == null || m[1].isBlank())
                .map(m -> m[0])
                .toList();
        assertThat(missing)
                .as("这些菜单派生不出 i18nKey（permission 与 path 都为空？）")
                .isEmpty();

        // key 必须是 menu.* 且无重复
        List<String> keys = menus.stream().map(m -> m[1]).toList();
        assertThat(keys).allMatch(k -> k.startsWith("menu."));
        assertThat(new java.util.HashSet<>(keys))
                .as("派生 key 不应重复，重复意味着两个菜单共用一条译文")
                .hasSameSizeAs(keys);
    }

    @Test
    @DisplayName("用户设置过语言时_UserInfoVO下发language字段")
    void 用户设置过语言时_UserInfoVO下发language字段() throws Exception {
        String userAuth = loginWithLanguage("en_US");
        MvcResult result = mockMvc.perform(bareGet("/api/v1/auth/user-info")
                        .header("Authorization", userAuth))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(result).at("/data/language").asText())
                .as("显式选过语言的用户，前端要据此在渲染业务页面前切好语言，避免闪一下")
                .isEqualTo("en_US");
    }

    @Test
    @DisplayName("permission与path皆空的菜单_i18nKey为null且接口不报错")
    void permission与path皆空的菜单_i18nKey为null且接口不报错() throws Exception {
        // 建一个目录菜单，permission 与 path 都不填 → 派生不出 key → 前端回退显示 name
        Map<String, Object> body = new HashMap<>();
        body.put("parentId", 0);
        body.put("name", "无编码目录" + SEQ.incrementAndGet());
        body.put("type", 1);
        body.put("sort", 900);
        MvcResult created = mockMvc.perform(authedPost("/api/v1/menus").content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode vo = parse(created).at("/data");
        assertThat(vo.path("i18nKey").isNull() || vo.path("i18nKey").asText("").isEmpty())
                .as("派生不出 key 时必须是 null，而不是空串或畸形 key")
                .isTrue();
        assertThat(vo.path("name").asText()).startsWith("无编码目录");
    }

    @Test
    @DisplayName("菜单树接口下发i18nKey_与permission派生一致")
    void 菜单树接口下发i18nKey_与permission派生一致() throws Exception {
        MvcResult result = mockMvc.perform(bareGet("/api/v1/menus/tree")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = parse(result).at("/data");

        int checked = 0;
        for (JsonNode node : flatten(data)) {
            String perm = node.path("permission").asText(null);
            String path = node.path("path").asText(null);
            String expected = com.gentry.core.i18n.MenuI18nKeyResolver.resolve(perm, path);
            String actual = node.path("i18nKey").asText(null);
            assertThat(actual)
                    .as("菜单 %s 的 i18nKey 与派生规则不一致", node.path("name").asText())
                    .isEqualTo(expected);
            checked++;
        }
        assertThat(checked).as("菜单树不应为空").isPositive();
    }

    private List<JsonNode> flatten(JsonNode nodes) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode n : nodes) {
            out.add(n);
            if (n.has("children") && n.get("children").isArray()) {
                out.addAll(flatten(n.get("children")));
            }
        }
        return out;
    }

    @Test
    @DisplayName("字典接口下发i18nKey")
    void 字典接口下发i18nKey() throws Exception {
        MvcResult types = mockMvc.perform(authedGet("/api/v1/dict/types?pageNum=1&pageSize=50"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = parse(types).at("/data/list");
        assertThat(list.isArray()).isTrue();
        for (JsonNode t : list) {
            assertThat(t.path("i18nKey").asText())
                    .as("字典类型 %s 的 i18nKey", t.path("dictType").asText())
                    .isEqualTo("dict.type." + t.path("dictType").asText());
        }

        if (list.size() > 0) {
            String dictType = list.get(0).path("dictType").asText();
            MvcResult data = mockMvc.perform(authedGet("/api/v1/dict/types/{dictType}/data", dictType))
                    .andExpect(status().isOk())
                    .andReturn();
            for (JsonNode d : parse(data).at("/data")) {
                assertThat(d.path("i18nKey").asText())
                        .isEqualTo("dict." + dictType + "." + d.path("dictValue").asText());
            }
        }
    }

    // ==================== Bean Validation 消息 ====================

    @Test
    @DisplayName("校验失败提示_按语言本地化且不露裸key")
    void 校验失败提示_按语言本地化且不露裸key() throws Exception {
        // 空 body 触发 @NotBlank：username / nickname / password 均必填
        String empty = json(Map.of());

        MvcResult en = mockMvc.perform(authedPost("/api/v1/users")
                        .header("Accept-Language", "en-US")
                        .content(empty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andReturn();
        String enMsg = parse(en).at("/message").asText();
        assertThat(enMsg)
                .as("Bean Validation 的 {key} 缺失会原样输出，没有 defaultValue 可退")
                .doesNotContain("{").doesNotContain("}")
                .contains("Username is required");

        MvcResult zh = mockMvc.perform(authedPost("/api/v1/users")
                        .header("Accept-Language", "zh-CN")
                        .content(empty))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(zh).at("/message").asText())
                .doesNotContain("{").contains("用户名不能为空");
    }

    @Test
    @DisplayName("校验消息里的BV属性插值_max仍被替换")
    void 校验消息里的BV属性插值_max仍被替换() throws Exception {
        // @Size 的 {max} 由 Hibernate Validator 自己插值，与 MessageSource 无关；
        // 译文里若写 {max} 必须被替换成数字，不能原样输出
        Map<String, Object> body = new HashMap<>();
        body.put("username", "toolongusername_exceeding_limit");
        body.put("nickname", "n");
        body.put("password", "Abc@12345");
        MvcResult result = mockMvc.perform(authedPost("/api/v1/users")
                        .header("Accept-Language", "en-US")
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(result).at("/message").asText())
                .as("出现裸 {max} 说明属性插值被破坏")
                .doesNotContain("{max}").doesNotContain("{min}");
    }

    @Test
    @DisplayName("未带message的校验注解_仍走HibernateValidator自带多语言默认消息")
    void 未带message的校验注解_仍走HibernateValidator自带多语言默认消息() throws Exception {
        // ConfigCreateDTO.configName 是 @Size(max = 100) 且 **不带 message**。
        // 关注点：把 MessageSource 接进 LocalValidatorFactoryBean 之后，
        // HV 自己的 ValidationMessages 兜底不能被顶掉 —— 否则这类注解会退化成裸 key。
        Map<String, Object> body = new HashMap<>();
        body.put("configKey", "i18n.probe.key");
        body.put("configName", "n".repeat(120));   // 超 100，触发无 message 的 @Size

        MvcResult en = mockMvc.perform(authedPost("/api/v1/configs")
                        .header("Accept-Language", "en-US")
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andReturn();
        String enMsg = parse(en).at("/message").asText();
        assertThat(enMsg)
                .as("HV 默认消息必须仍被本地化，且不出现裸占位符")
                .doesNotContain("{").doesNotContain("}")
                .containsIgnoringCase("size must be between");

        MvcResult zh = mockMvc.perform(authedPost("/api/v1/configs")
                        .header("Accept-Language", "zh-CN")
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(parse(zh).at("/message").asText())
                .as("HV 自带 zh_CN 资源包应生效")
                .doesNotContain("{")
                .isNotEqualTo(enMsg);
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
