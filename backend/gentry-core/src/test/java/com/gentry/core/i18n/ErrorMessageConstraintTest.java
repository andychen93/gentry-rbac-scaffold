package com.gentry.core.i18n;

import com.gentry.core.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误消息的约束型测试（防线，红灯即阻塞提交）。
 *
 * <p>对应详细设计的 C-01 / C-01b / C-07 / C-08。这几条比功能测试更重要——它们防的是
 * <b>「漏改一处，只在切换语言时才暴露」</b>这类在中文环境下永远测不出来的缺陷。</p>
 */
class ErrorMessageConstraintTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");

    /** 是否是注释行。文档与 Javadoc 里写有示例代码，不跳过会误报 */
    private static boolean isComment(String line) {
        String t = line.strip();
        return t.startsWith("//") || t.startsWith("*") || t.startsWith("/*");
    }

    private Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("i18n/" + name)) {
            assertThat(in).as("资源文件缺失: i18n/%s", name).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    // ==================== C-01 ====================

    @Test
    @DisplayName("每个ErrorCode的i18nKey在中英文资源文件中都存在")
    void 每个ErrorCode的i18nKey在中英文资源文件中都存在() throws IOException {
        Properties zh = load("error_zh_CN.properties");
        Properties en = load("error_en_US.properties");

        List<String> missingZh = new ArrayList<>();
        List<String> missingEn = new ArrayList<>();
        for (ErrorCode ec : ErrorCode.values()) {
            String key = ec.i18nKey();
            if (!zh.containsKey(key)) missingZh.add(ec.name() + " -> " + key);
            if (!en.containsKey(key)) missingEn.add(ec.name() + " -> " + key);
        }
        assertThat(missingZh).as("error_zh_CN.properties 缺少这些 ErrorCode 的译文").isEmpty();
        assertThat(missingEn).as("error_en_US.properties 缺少这些 ErrorCode 的译文").isEmpty();
    }

    @Test
    @DisplayName("i18nKey派生规则_下划线转点且全小写")
    void i18nKey派生规则_下划线转点且全小写() {
        assertThat(ErrorCode.ROLE_NOT_FOUND.i18nKey()).isEqualTo("error.role.not.found");
        assertThat(ErrorCode.USER_NOT_FOUND.i18nKey()).isEqualTo("error.user.not.found");
        assertThat(ErrorCode.HTTP_MESSAGE_NOT_READABLE.i18nKey())
                .isEqualTo("error.http.message.not.readable");
    }

    @Test
    @DisplayName("i18nKey全局唯一_无枚举项撞key")
    void i18nKey全局唯一_无枚举项撞key() {
        Set<String> keys = new LinkedHashSet<>();
        List<String> dup = new ArrayList<>();
        for (ErrorCode ec : ErrorCode.values()) {
            if (!keys.add(ec.i18nKey())) dup.add(ec.name() + " -> " + ec.i18nKey());
        }
        assertThat(dup).as("不同枚举名派生出了相同的 key").isEmpty();
    }

    // ==================== C-01b ====================

    /**
     * 扫描所有 {@code BizException(ErrorCode.X, "...")} 的第二参数。
     *
     * <p>本项目决定该参数是 <b>i18n key 而非文案</b>（见概要设计第 5 问）。key 拼错不会
     * 编译报错，只会在切换语言时显示成 key 本身——这条测试是唯一的自动化防线。</p>
     */
    @Test
    @DisplayName("BizException第二参数必须是error开头的key且资源文件中存在")
    void BizException第二参数必须是error开头的key且资源文件中存在() throws IOException {
        Path backendRoot = locateBackendRoot();
        Properties zh = load("error_zh_CN.properties");
        Properties en = load("error_en_US.properties");

        Pattern call = Pattern.compile("BizException\\(ErrorCode\\.[A-Z_]+,\\s*\"([^\"]+)\"");
        List<String> violations = new ArrayList<>();

        for (Path java : sourceFiles(backendRoot)) {
            List<String> lines = Files.readAllLines(java, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                if (isComment(lines.get(i))) continue;
                Matcher m = call.matcher(lines.get(i));
                while (m.find()) {
                    String key = m.group(1);
                    String at = backendRoot.relativize(java) + ":" + (i + 1) + "  " + key;
                    if (!key.startsWith("error.")) {
                        violations.add(at + "  ← 不是 error. 开头（是不是漏改成裸文案了？）");
                    } else if (!zh.containsKey(key)) {
                        violations.add(at + "  ← error_zh_CN.properties 里没有这个 key");
                    } else if (!en.containsKey(key)) {
                        violations.add(at + "  ← error_en_US.properties 里没有这个 key");
                    }
                }
            }
        }
        assertThat(violations).as("BizException 的 messageKey 违规").isEmpty();
    }

    // ==================== C-07 ====================

    /**
     * {@code MessageFormat} 把单引号当转义符：带 {@code {0}} 占位的译文里若出现未成对的
     * 单引号，占位符会<b>原样输出且不报错</b>（例如 {@code Role doesn't exist: {0}}）。
     * 这是静默 bug，只能靠扫描防住。
     */
    @Test
    @DisplayName("带占位符的译文中单引号必须成对")
    void 带占位符的译文中单引号必须成对() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String file : List.of("error.properties", "error_zh_CN.properties", "error_en_US.properties",
                "messages.properties", "messages_zh_CN.properties", "messages_en_US.properties")) {
            Properties p = load(file);
            for (String key : p.stringPropertyNames()) {
                String value = p.getProperty(key);
                if (!PLACEHOLDER.matcher(value).find()) {
                    continue;   // 无占位符则不走 MessageFormat，单引号无害
                }
                long singles = countUnescapedSingleQuotes(value);
                if (singles % 2 != 0) {
                    violations.add(file + " / " + key + " = " + value
                            + "  ← 含 {n} 占位且单引号未成对，占位符将不被替换（写成 '' 转义）");
                }
            }
        }
        assertThat(violations).as("MessageFormat 单引号陷阱").isEmpty();
    }

    /** 统计非 '' 形式的单引号个数 */
    private long countUnescapedSingleQuotes(String v) {
        long n = 0;
        for (int i = 0; i < v.length(); i++) {
            if (v.charAt(i) != '\'') continue;
            if (i + 1 < v.length() && v.charAt(i + 1) == '\'') {
                i++;      // '' 成对，跳过
            } else {
                n++;
            }
        }
        return n;
    }

    // ==================== C-08 ====================

    @Test
    @DisplayName("每个basename的中英文key集合完全一致")
    void 每个basename的中英文key集合完全一致() throws IOException {
        for (String base : List.of("error", "messages")) {
            Properties zh = load(base + "_zh_CN.properties");
            Properties en = load(base + "_en_US.properties");
            Set<String> onlyZh = new TreeSet<>(zh.stringPropertyNames());
            onlyZh.removeAll(en.stringPropertyNames());
            Set<String> onlyEn = new TreeSet<>(en.stringPropertyNames());
            onlyEn.removeAll(zh.stringPropertyNames());
            assertThat(onlyZh).as("%s: 中文有但英文缺（漏译）", base).isEmpty();
            assertThat(onlyEn).as("%s: 英文有但中文缺（孤儿）", base).isEmpty();
        }
    }

    @Test
    @DisplayName("无后缀资源包与zhCN内容一致_保证最终兜底")
    void 无后缀资源包与zhCN内容一致_保证最终兜底() throws IOException {
        // fallback-to-system-locale=false 时，无后缀包是最终兜底，必须与 zh_CN 等价
        for (String base : List.of("error", "messages")) {
            Properties none = load(base + ".properties");
            Properties zh = load(base + "_zh_CN.properties");
            assertThat(none.stringPropertyNames())
                    .as("%s.properties 与 %s_zh_CN.properties 的 key 集合应一致", base, base)
                    .isEqualTo(zh.stringPropertyNames());
        }
    }

    /**
     * 资源文件里的每个 basename 都必须在 {@code spring.messages.basename} 里登记。
     *
     * <p>这条是补的盲区：本类其余测试都是<b>直接读 properties 文件</b>，所以「文件存在且内容
     * 正确」全绿，但 {@code MessageSource} 根本加载不到——首次实现时正是这样，
     * {@code error_*.properties} 建好了却漏登记 basename，症状是接口全量返回中文兜底、
     * 不报任何错，直到集成测试才暴露。</p>
     *
     * <p>MessageSource 层面的实际取值验证在 {@code I18nApiIT}（需要 Spring 上下文）。</p>
     */
    @Test
    @DisplayName("每个资源文件的basename都已在applicationyml中登记")
    void 每个资源文件的basename都已在applicationyml中登记() throws IOException {
        Path appYml = locateBackendRoot().resolve("gentry-start/src/main/resources/application.yml");
        assertThat(Files.exists(appYml)).as("找不到 application.yml").isTrue();
        String yml = Files.readString(appYml, StandardCharsets.UTF_8);

        Matcher m = Pattern.compile("basename:\\s*(\\S+)").matcher(yml);
        assertThat(m.find()).as("application.yml 里没有 spring.messages.basename").isTrue();
        Set<String> declared = new java.util.LinkedHashSet<>(List.of(m.group(1).split(",")));

        // 扫 i18n 目录下的所有 basename（去掉 _zh_CN / _en_US 后缀）
        Path i18nDir = locateBackendRoot().resolve("gentry-core/src/main/resources/i18n");
        Set<String> actual = new java.util.TreeSet<>();
        try (Stream<Path> s = Files.list(i18nDir)) {
            s.filter(f -> f.getFileName().toString().endsWith(".properties")).forEach(f -> {
                String n = f.getFileName().toString().replace(".properties", "")
                        .replaceAll("_(zh_CN|en_US)$", "");
                actual.add("i18n/" + n);
            });
        }
        Set<String> unregistered = new java.util.TreeSet<>(actual);
        unregistered.removeAll(declared);
        assertThat(unregistered)
                .as("这些资源包存在但没登记到 spring.messages.basename，MessageSource 加载不到")
                .isEmpty();
    }

    /**
     * 应用日志一律英文固定文本，不参与 i18n。
     *
     * <p>理由是确定性：日志的读者是运维，若 {@code log.info("用户 {} 登录成功")} 参与 i18n，
     * grep 日志得先知道当时哪个 locale 生效。固定英文让 grep 有唯一答案。</p>
     */
    @Test
    @DisplayName("日志语句不得包含中文")
    void 日志语句不得包含中文() throws IOException {
        Pattern logCall = Pattern.compile("\\blog\\s*\\.\\s*(info|debug|warn|error|trace)\\s*\\(\\s*\"([^\"]*)\"");
        Pattern cjk = Pattern.compile("[\\u4e00-\\u9fff]");
        Path root = locateBackendRoot();

        List<String> violations = new ArrayList<>();
        for (Path java : sourceFiles(root)) {
            List<String> lines = Files.readAllLines(java, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                if (isComment(lines.get(i))) continue;
                Matcher m = logCall.matcher(lines.get(i));
                while (m.find()) {
                    if (cjk.matcher(m.group(2)).find()) {
                        violations.add(root.relativize(java) + ":" + (i + 1) + "  " + m.group(2));
                    }
                }
            }
        }
        assertThat(violations)
                .as("这些日志语句还是中文；日志固定英文以保证 grep 的确定性")
                .isEmpty();
    }

    @Test
    @DisplayName("译文非空且无TODO残留")
    void 译文非空且无TODO残留() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String file : List.of("error_zh_CN.properties", "error_en_US.properties")) {
            Properties p = load(file);
            for (String key : p.stringPropertyNames()) {
                String v = p.getProperty(key);
                if (v == null || v.isBlank()) violations.add(file + " / " + key + " 为空");
                if (v != null && v.contains("TODO")) violations.add(file + " / " + key + " 含 TODO");
            }
        }
        assertThat(violations).isEmpty();
    }

    // ==================== 辅助 ====================

    /** 从测试工作目录向上找到含 gentry-core 的 backend 根目录 */
    private Path locateBackendRoot() {
        Path p = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && p != null; i++, p = p.getParent()) {
            if (Files.isDirectory(p.resolve("gentry-core")) && Files.isDirectory(p.resolve("gentry-business"))) {
                return p;
            }
        }
        throw new IllegalStateException("找不到 backend 根目录（应含 gentry-core 与 gentry-business）");
    }

    private List<Path> sourceFiles(Path backendRoot) throws IOException {
        List<Path> all = new ArrayList<>();
        for (String mod : List.of("gentry-core", "gentry-business", "gentry-monitor")) {
            Path base = backendRoot.resolve(mod).resolve("src/main/java");
            if (!Files.isDirectory(base)) continue;
            try (Stream<Path> s = Files.walk(base)) {
                s.filter(f -> f.toString().endsWith(".java")).forEach(all::add);
            }
        }
        return all;
    }
}
