package com.gentry.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.mapper.UserMapper;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
     * 改动随用例回滚；但用新建用户能顺带保证「其他用例看到的种子数据没被动过」。
     * 这里一律按主键更新。</p>
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
    @DisplayName("新建用户的language为NULL_V12不填默认值")
    void 新建用户的language为NULL_V12不填默认值() throws Exception {
        /*
         * V12 迁移刻意不填默认值：NOT NULL DEFAULT 'zh_CN' 会让三级链的第 ② 级永不执行。
         *
         * **断言对象是新建的用户，不是种子用户。**
         * 原来这里遍历 chenli/admin/zhangsan 断言 language 为 NULL —— 那是在断言一行
         * **任何人都能改**的可变数据：用户在界面上点一次语言切换就会落库，这条用例随即变红，
         * 而迁移本身毫无问题。UI E2E 的 I18N-005 正是这么把它打红的（它用 zhangsan 走真实
         * 切换以覆盖「偏好落库」路径）。
         * 「新建用户不带语言偏好」才是这条迁移真正要保证的语义，且不受任何人操作影响。
         */
        String username = "i18nDefault" + SEQ.incrementAndGet();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("nickname", "国际化测试用户");
        body.put("password", TEST_PWD);
        body.put("deptId", 100);
        body.put("status", 1);
        mockMvc.perform(authedPost("/api/v1/users").content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        User created = userMapper.selectByUsername(username);
        assertThat(created).as("刚建的用户应存在").isNotNull();
        assertThat(created.getLanguage())
                .as("新建用户的 language 必须是 NULL（= 从未选过，跟随浏览器）")
                .isNull();
    }

    // ==================== 导出 / 导入 ====================

    @Test
    @DisplayName("导出Excel表头随语言变化")
    void 导出Excel表头随语言变化() throws Exception {
        byte[] en = mockMvc.perform(authedGet("/api/v1/users/export?pageNum=1&pageSize=10")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        byte[] zh = mockMvc.perform(authedGet("/api/v1/users/export?pageNum=1&pageSize=10")
                        .header("Accept-Language", "zh-CN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(readFirstRow(en))
                .as("英文表头必须是译文，不能是 @ExcelProperty 里的中文常量")
                .containsExactly("Username", "Nickname", "Phone", "Email",
                        "Gender", "Department", "Job title", "Status", "Created at");
        assertThat(readFirstRow(zh))
                .containsExactly("用户名", "昵称", "手机号", "邮箱",
                        "性别", "部门", "职务", "状态", "创建时间");
    }

    @Test
    @DisplayName("英文导出的Excel能原样导回来_按列序匹配")
    void 英文导出的Excel能原样导回来_按列序匹配() throws Exception {
        // 这条是批次 6「内部不可拆」的证据：只做导出本地化而不改导入的列绑定方式，
        // 英文文件导回来会因为表头对不上而静默得到全 null。
        String username = "imp" + SEQ.incrementAndGet();
        byte[] xlsx = buildImportFile(new String[]{
                "Username", "Nickname", "Phone", "Email", "Gender", "Job title"},
                new String[]{username, "导入昵称", "", "", "1", "工程师"});

        MvcResult result = mockMvc.perform(multipart("/api/v1/users/import")
                        .file(new MockMultipartFile("file", "users_en.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                        .header("Authorization", auth)
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = parse(result).at("/data");
        assertThat(data.path("successCount").asInt() + data.path("success").asInt())
                .as("英文表头的文件应能成功导入 1 行，得到 0 说明列没匹配上。响应=%s", data)
                .isPositive();
    }

    @Test
    @DisplayName("导入错误项按语言本地化_不露裸key")
    void 导入错误项按语言本地化_不露裸key() throws Exception {
        // 第一列留空触发 error.user.import.username.blank
        byte[] xlsx = buildImportFile(
                new String[]{"Username", "Nickname", "Phone", "Email", "Gender", "Job title"},
                new String[]{"", "无用户名", "", "", "0", ""});

        MvcResult result = mockMvc.perform(multipart("/api/v1/users/import")
                        .file(new MockMultipartFile("file", "bad.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                        .header("Authorization", auth)
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body)
                .as("BizException.getMessage() 现在返回 i18n key，导入错误项必须先本地化再给用户")
                .doesNotContain("error.user.import");
    }

    @Test
    @DisplayName("种子用户的职务是字典码_而非中文label")
    void 种子用户的职务是字典码_而非中文label() throws Exception {
        /*
         * V13 之前 sys_user.post_name 存的是「经理」这种中文 label，导致职务下拉的 value
         * 必须是中文、因而无法翻译。这条钉住迁移的结果：库里存的必须是 sys_user_post 的码。
         *
         * 断言「在字典码集合内」而不是逐个写死 chenli=ChiefArchitect —— 后者会在有人
         * 改了某个种子用户的职务时变红，而那不是缺陷。
         */
        MvcResult result = mockMvc.perform(authedGet("/api/v1/dict/types/sys_user_post/data"))
                .andExpect(status().isOk())
                .andReturn();
        Set<String> codes = new HashSet<>();
        for (JsonNode d : parse(result).at("/data")) {
            codes.add(d.path("dictValue").asText());
        }
        assertThat(codes).as("sys_user_post 字典应有内容").isNotEmpty();
        assertThat(codes).as("「司机 / Driver」是车辆定位平台的残留，V13 已删").doesNotContain("Driver");

        for (String seed : new String[]{"chenli", "admin", "zhangsan"}) {
            User u = userMapper.selectByUsername(seed);
            assertThat(u).as("种子用户 %s 应存在", seed).isNotNull();
            assertThat(u.getPostName())
                    .as("种子用户 %s 的 post_name 应是字典码而不是中文 label", seed)
                    .isIn(codes);
        }
    }

    @Test
    @DisplayName("导出的Excel能原样导回_性别与职务都落对")
    void 导出的Excel能原样导回_性别与职务都落对() throws Exception {
        /*
         * 覆盖一个既有缺陷：导出写「男」/「经理」（给人看），而导入侧原来 gender 是 Integer、
         * postName 直接当字符串塞库，于是**导出的文件导回来**性别静默变 null、
         * 职务变成一个查不到字典的孤值。现在两列都由 LocalizedCodeResolver 归一。
         *
         * 导出断言跑两种语言（GET，无副作用）；**导回只做一次**：导入接口有
         * @RepeatSubmit(interval = 5)，而它的指纹是 userId|method|uri|argsMD5 且
         * **MultipartFile 不参与 MD5**（RepeatSubmitAspect.extractArgs 跳过不可序列化对象），
         * 所以同一用户 5 秒内的两次导入指纹完全相同、第二次必被判重复提交。
         * 「英文 label 也能认」由 LocalizedCodeResolverTest 直接测那个纯函数。
         *
         * 导入用**另一个账号**：本类里已有两条用例在打这个接口，用 admin 会撞上它们的
         * 5 秒窗口（指纹只差 userId）。
         */
        String username = "roundtrip" + SEQ.incrementAndGet();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("nickname", "往返测试");
        body.put("password", TEST_PWD);
        body.put("deptId", 100);
        body.put("gender", 1);
        body.put("postName", "Manager");
        body.put("status", 1);
        mockMvc.perform(authedPost("/api/v1/users").content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, List<String>> exported = new HashMap<>();
        for (String lang : new String[]{"zh-CN", "en-US"}) {
            // pageSize 上限是 100（valid.common.pageSize.max）
            byte[] xlsx = mockMvc.perform(
                            authedGet("/api/v1/users/export?pageNum=1&pageSize=100&username=" + username)
                                    .header("Accept-Language", lang))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();

            // 先确认真是 xlsx：全局异常处理器对参数错误也返回 HTTP 200 + JSON，
            // 直接丢给 EasyExcel 会得到「invalid char between encapsulated token」这种无从下手的报错
            assertThat(new String(xlsx, 0, Math.min(4, xlsx.length),
                    java.nio.charset.StandardCharsets.ISO_8859_1))
                    .as("导出接口没有返回 xlsx，实际响应=%s",
                            new String(xlsx, java.nio.charset.StandardCharsets.UTF_8))
                    .startsWith("PK");

            List<String> row = readRowByFirstCell(xlsx, username);
            exported.put(lang, row);
            // 导出是给人看的：性别与职务列必须是当前语言的展示名，不是码
            assertThat(row.get(4))
                    .as("%s 导出的性别列应是展示名", lang)
                    .isEqualTo("zh-CN".equals(lang) ? "男" : "Male");
            assertThat(row.get(6))
                    .as("%s 导出的职务列应是展示名", lang)
                    .isEqualTo("zh-CN".equals(lang) ? "经理" : "Manager");
        }

        // 拿中文导出的那一行原样导回。导入列序：username/nickname/phone/email/gender/postName
        List<String> row = exported.get("zh-CN");
        String reimported = username + "re";
        byte[] back = buildImportFile(
                new String[]{"c0", "c1", "c2", "c3", "c4", "c5"},
                new String[]{reimported, row.get(1), row.get(2), row.get(3), row.get(4), row.get(6)});
        String importerAuth = loginWithLanguage(null);
        mockMvc.perform(multipart("/api/v1/users/import")
                        .file(new MockMultipartFile("file", "back.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", back))
                        .header("Authorization", importerAuth)
                        .header("Accept-Language", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        User got = userMapper.selectByUsername(reimported);
        assertThat(got).as("导出的文件应能导回来").isNotNull();
        assertThat(got.getGender()).as("往返后性别应还是 1（男）").isEqualTo(1);
        assertThat(got.getPostName()).as("往返后职务应归一成字典码").isEqualTo("Manager");
    }

    @Test
    @DisplayName("导入模板表头随语言变化")
    void 导入模板表头随语言变化() throws Exception {
        byte[] en = mockMvc.perform(authedGet("/api/v1/users/import/template")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(readFirstRow(en)).contains("Username", "Nickname");
    }

    /** 读 xlsx 首行（表头） */
    private List<String> readFirstRow(byte[] xlsx) {
        List<String> head = new ArrayList<>();
        com.alibaba.excel.EasyExcel.read(new java.io.ByteArrayInputStream(xlsx))
                .sheet()
                .headRowNumber(0)
                .registerReadListener(new com.alibaba.excel.read.listener.ReadListener<Map<Integer, String>>() {
                    private boolean captured = false;

                    @Override
                    public void invoke(Map<Integer, String> row,
                                       com.alibaba.excel.context.AnalysisContext ctx) {
                        if (!captured) {
                            new java.util.TreeMap<>(row).values().forEach(head::add);
                            captured = true;
                        }
                    }

                    @Override
                    public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext ctx) {
                    }
                })
                .doRead();
        return head;
    }

    /** 读 xlsx 里 username 等于给定值的那一行（表头之后的数据行） */
    private List<String> readRowByFirstCell(byte[] xlsx, String firstCell) {
        List<List<String>> rows = new ArrayList<>();
        com.alibaba.excel.EasyExcel.read(new java.io.ByteArrayInputStream(xlsx))
                .sheet()
                .headRowNumber(0)
                .registerReadListener(new com.alibaba.excel.read.listener.ReadListener<Map<Integer, String>>() {
                    @Override
                    public void invoke(Map<Integer, String> row,
                                       com.alibaba.excel.context.AnalysisContext ctx) {
                        List<String> cells = new ArrayList<>();
                        // TreeMap 按列序补齐：EasyExcel 对空单元格不产生 key，直接取 values 会错位
                        java.util.TreeMap<Integer, String> sorted = new java.util.TreeMap<>(row);
                        int max = sorted.isEmpty() ? -1 : sorted.lastKey();
                        for (int i = 0; i <= max; i++) {
                            cells.add(sorted.getOrDefault(i, ""));
                        }
                        rows.add(cells);
                    }
                    @Override
                    public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext ctx) {
                    }
                })
                .doRead();
        return rows.stream()
                .filter(r -> !r.isEmpty() && firstCell.equals(r.get(0)))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "导出的 Excel 里找不到 username=" + firstCell + " 的行，实际行数=" + rows.size()));
    }

    /** 用给定表头与一行数据造一个 xlsx */
    private byte[] buildImportFile(String[] head, String[] row) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        List<List<String>> heads = new ArrayList<>();
        for (String h : head) {
            heads.add(List.of(h));
        }
        List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<>(java.util.Arrays.asList((Object[]) row)));
        com.alibaba.excel.EasyExcel.write(out).head(heads).sheet("Sheet1").doWrite(data);
        return out.toByteArray();
    }

    // ==================== 语言偏好接口 ====================

    @Test
    @DisplayName("切换语言_落库并同步Session_后续请求立即生效")
    void 切换语言_落库并同步Session_后续请求立即生效() throws Exception {
        String userAuth = loginWithLanguage(null);   // 初始未设置，跟随请求头

        mockMvc.perform(barePut("/api/v1/users/me/language")
                        .header("Authorization", userAuth)
                        .content(json(Map.of("language", "en_US"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 不重新登录，直接发下一个请求：Session 已同步则 ① 生效，即使请求头是 zh-CN
        assertThat(deleteAbsentRole("zh-CN", userAuth).at("/message").asText())
                .as("Session 不同步的话，导出/定时通知会一直用旧语言直到重新登录")
                .isEqualTo("Role not found");

        // 落库校验
        MvcResult info = mockMvc.perform(bareGet("/api/v1/auth/user-info")
                        .header("Authorization", userAuth))
                .andReturn();
        assertThat(parse(info).at("/data/language").asText()).isEqualTo("en_US");
    }

    @Test
    @DisplayName("切换语言_白名单外的值被拒绝")
    void 切换语言_白名单外的值被拒绝() throws Exception {
        // 格式合法（@Pattern 通过）但不在 gentry.i18n.supported-locales 里
        MvcResult result = mockMvc.perform(authedPut("/api/v1/users/me/language")
                        .header("Accept-Language", "en-US")
                        .content(json(Map.of("language", "ja_JP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002))
                .andReturn();
        assertThat(parse(result).at("/message").asText())
                .as("消息应本地化且带上非法值")
                .isEqualTo("Unsupported language: ja_JP");
    }

    @Test
    @DisplayName("切换语言_格式非法被DTO拦下")
    void 切换语言_格式非法被DTO拦下() throws Exception {
        mockMvc.perform(authedPut("/api/v1/users/me/language")
                        .content(json(Map.of("language", "not-a-locale!!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("语言列表接口_未登录也可访问")
    void 语言列表接口_未登录也可访问() throws Exception {
        // 登录页就要渲染语言选择器。只写 @RestController 是不够的 ——
        // SaTokenConfig 的登录校验拦截器覆盖 /api/**，必须加进 excludePathPatterns。
        // 实测漏加时这里返回 401 + code 30001。
        mockMvc.perform(bareGet("/api/v1/i18n/locales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("语言列表接口_返回自称且不随locale变化")
    void 语言列表接口_返回自称且不随locale变化() throws Exception {
        for (String lang : new String[]{"zh-CN", "en-US"}) {
            MvcResult result = mockMvc.perform(bareGet("/api/v1/i18n/locales")
                            .header("Accept-Language", lang))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            JsonNode list = parse(result).at("/data");
            assertThat(list).hasSize(2);
            // label 是自称，不翻译：英文用户看到「中文」反而不认识
            assertThat(list.get(0).path("code").asText()).isEqualTo("zh_CN");
            assertThat(list.get(0).path("label").asText()).isEqualTo("简体中文");
            assertThat(list.get(1).path("label").asText()).isEqualTo("English");
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
        MvcResult result = mockMvc.perform(authedGet("/api/v1/auth/user-info"))
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
        assertThat(menus).as("应能看到导航菜单").isNotEmpty();

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
