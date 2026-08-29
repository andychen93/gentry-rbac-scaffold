package com.gentry.core.i18n;

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
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验消息的约束型测试（C-02）。
 *
 * <p><b>这一批没有兜底机制，所以本测试是唯一防线。</b>Bean Validation 解析
 * {@code message = "{valid.xxx}"} 时，若 key 不存在会把 <code>{valid.xxx}</code>
 * <b>原样输出给终端用户</b>——不像 {@code I18nUtil} 有 defaultValue 可退。
 * 因此 key 拼错、漏译、或漏登记 basename 都必须在提交前红灯。</p>
 */
class ValidationMessageConstraintTest {

    /** 匹配 message = "{valid.xxx}" 里的 key */
    private static final Pattern KEY_IN_MESSAGE =
            Pattern.compile("message\\s*=\\s*\"\\{([^}\"]+)}\"");
    /** 匹配 message = "任意非 {} 包裹的字面量" —— 这类是漏改 */
    private static final Pattern LITERAL_MESSAGE =
            Pattern.compile("message\\s*=\\s*\"(?!\\{)([^\"]*)\"");
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    /**
     * 是否是注释行。
     *
     * <p>必须跳过：本项目的 Javadoc 里就写着 {@code message = "{valid.xxx}"} 这样的示例，
     * 不跳过会把文档当成代码扫出误报（首次跑本测试就是这么红的）。</p>
     */
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

    @Test
    @DisplayName("校验注解引用的每个key在中英文资源文件中都存在")
    void 校验注解引用的每个key在中英文资源文件中都存在() throws IOException {
        Properties zh = load("validation_zh_CN.properties");
        Properties en = load("validation_en_US.properties");
        Path root = locateBackendRoot();

        List<String> violations = new ArrayList<>();
        int scanned = 0;
        for (Path java : sourceFiles(root)) {
            List<String> lines = Files.readAllLines(java, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                if (isComment(lines.get(i))) continue;
                Matcher m = KEY_IN_MESSAGE.matcher(lines.get(i));
                while (m.find()) {
                    String key = m.group(1);
                    scanned++;
                    String at = root.relativize(java) + ":" + (i + 1) + "  {" + key + "}";
                    if (!key.startsWith("valid.")) {
                        // BV 自带的 {jakarta.validation...} 或 {max} 这类属性插值不在此列
                        continue;
                    }
                    if (!zh.containsKey(key)) {
                        violations.add(at + "  ← validation_zh_CN.properties 里没有");
                    } else if (!en.containsKey(key)) {
                        violations.add(at + "  ← validation_en_US.properties 里没有");
                    }
                }
            }
        }
        assertThat(scanned).as("一个 key 都没扫到，正则或路径有问题").isPositive();
        assertThat(violations).as("校验消息 key 缺失（会把 {valid.xxx} 原样显示给用户）").isEmpty();
    }

    @Test
    @DisplayName("校验注解里不得残留中文字面量message")
    void 校验注解里不得残留中文字面量message() throws IOException {
        Path root = locateBackendRoot();
        List<String> violations = new ArrayList<>();
        for (Path java : sourceFiles(root)) {
            List<String> lines = Files.readAllLines(java, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (isComment(line)) continue;
                // 只看校验注解所在行，避开 @Log(title="...") 之类
                if (!line.contains("@NotBlank") && !line.contains("@NotNull")
                        && !line.contains("@NotEmpty") && !line.contains("@Size")
                        && !line.contains("@Pattern") && !line.contains("@Min")
                        && !line.contains("@Max") && !line.contains("@Email")) {
                    continue;
                }
                Matcher m = LITERAL_MESSAGE.matcher(line);
                while (m.find()) {
                    if (CJK.matcher(m.group(1)).find()) {
                        violations.add(root.relativize(java) + ":" + (i + 1) + "  " + m.group(1));
                    }
                }
            }
        }
        assertThat(violations)
                .as("这些校验注解的 message 还是中文字面量，英文界面下不会被翻译")
                .isEmpty();
    }

    @Test
    @DisplayName("validation资源包的中英文key集合完全一致")
    void validation资源包的中英文key集合完全一致() throws IOException {
        Properties zh = load("validation_zh_CN.properties");
        Properties en = load("validation_en_US.properties");
        Set<String> onlyZh = new TreeSet<>(zh.stringPropertyNames());
        onlyZh.removeAll(en.stringPropertyNames());
        Set<String> onlyEn = new TreeSet<>(en.stringPropertyNames());
        onlyEn.removeAll(zh.stringPropertyNames());
        assertThat(onlyZh).as("中文有但英文缺（漏译）").isEmpty();
        assertThat(onlyEn).as("英文有但中文缺（孤儿）").isEmpty();
    }

    @Test
    @DisplayName("validation资源包无孤儿key_每个key都被源码引用")
    void validation资源包无孤儿key_每个key都被源码引用() throws IOException {
        Properties zh = load("validation_zh_CN.properties");
        Path root = locateBackendRoot();

        Set<String> used = new TreeSet<>();
        for (Path java : sourceFiles(root)) {
            for (String line : Files.readAllLines(java, StandardCharsets.UTF_8)) {
                if (isComment(line)) continue;
                Matcher m = KEY_IN_MESSAGE.matcher(line);
                while (m.find()) {
                    used.add(m.group(1));
                }
            }
        }
        Set<String> orphans = new TreeSet<>(zh.stringPropertyNames());
        orphans.removeAll(used);
        assertThat(orphans)
                .as("这些 key 在语言包里但源码已不引用 —— 改了注解忘了清语言包")
                .isEmpty();
    }

    @Test
    @DisplayName("无后缀validation包与zhCN一致_保证最终兜底")
    void 无后缀validation包与zhCN一致_保证最终兜底() throws IOException {
        assertThat(load("validation.properties").stringPropertyNames())
                .isEqualTo(load("validation_zh_CN.properties").stringPropertyNames());
    }

    // ==================== 辅助 ====================

    private Path locateBackendRoot() {
        Path p = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && p != null; i++, p = p.getParent()) {
            if (Files.isDirectory(p.resolve("gentry-core-spring-boot-starter")) && Files.isDirectory(p.resolve("gentry-business"))) {
                return p;
            }
        }
        throw new IllegalStateException("找不到 backend 根目录");
    }

    private List<Path> sourceFiles(Path backendRoot) throws IOException {
        List<Path> all = new ArrayList<>();
        for (String mod : List.of("gentry-core-spring-boot-starter", "gentry-business", "gentry-monitor")) {
            Path base = backendRoot.resolve(mod).resolve("src/main/java");
            if (!Files.isDirectory(base)) continue;
            try (Stream<Path> s = Files.walk(base)) {
                s.filter(f -> f.toString().endsWith(".java")).forEach(all::add);
            }
        }
        return all;
    }
}
