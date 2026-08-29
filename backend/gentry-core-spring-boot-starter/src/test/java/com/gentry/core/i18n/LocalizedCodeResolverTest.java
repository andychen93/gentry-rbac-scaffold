package com.gentry.core.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocalizedCodeResolver} 是「导出的文件能导回来」这条性质的实现，
 * 用真实的资源包测，不打桩 MessageSource —— 打桩会把「译文到底存不存在」这个
 * 最容易出错的部分测掉。
 */
class LocalizedCodeResolverTest {

    private LocalizedCodeResolver resolver;

    /** 性别：码不是字典驱动的，i18n key 也不是 prefix + code 的形状 */
    private static final Map<String, String> GENDER = Map.of(
            "0", "export.user.gender.unknown",
            "1", "export.user.gender.male",
            "2", "export.user.gender.female");

    /** 职务：码来自 sys_user_post 字典，key 是 prefix + code */
    private static final Map<String, String> POST = posts(
            "CEO", "Director", "Manager", "Supervisor",
            "ChiefArchitect", "SeniorEngineer", "Engineer");

    private static Map<String, String> posts(String... codes) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String c : codes) {
            map.put(c, "export.user.post." + c);
        }
        return map;
    }

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasenames("i18n/export");
        ms.setDefaultEncoding("UTF-8");
        I18nProperties props = new I18nProperties();
        props.setSupportedLocales(List.of("zh_CN", "en_US"));
        resolver = new LocalizedCodeResolver(new I18nUtil(ms), props);
    }

    @Test
    @DisplayName("码原样输入_直接命中")
    void 码原样输入_直接命中() {
        assertThat(resolver.resolve(POST, "Manager")).isEqualTo("Manager");
        assertThat(resolver.resolve(GENDER, "1")).isEqualTo("1");
    }

    @Test
    @DisplayName("码大小写无关_运营手填也认")
    void 码大小写无关_运营手填也认() {
        assertThat(resolver.resolve(POST, "manager")).isEqualTo("Manager");
        assertThat(resolver.resolve(POST, "SENIORENGINEER")).isEqualTo("SeniorEngineer");
    }

    @Test
    @DisplayName("中文label命中_中文导出的文件能导回")
    void 中文label命中_中文导出的文件能导回() {
        assertThat(resolver.resolve(POST, "经理")).isEqualTo("Manager");
        assertThat(resolver.resolve(POST, "高级工程师")).isEqualTo("SeniorEngineer");
        assertThat(resolver.resolve(GENDER, "男")).isEqualTo("1");
        assertThat(resolver.resolve(GENDER, "未知")).isEqualTo("0");
    }

    @Test
    @DisplayName("英文label命中_英文导出的文件能导回")
    void 英文label命中_英文导出的文件能导回() {
        // 「Chief Architect」带空格，与码 ChiefArchitect 不同 —— 这正是「码相等」兜不住的情形
        assertThat(resolver.resolve(POST, "Chief Architect")).isEqualTo("ChiefArchitect");
        assertThat(resolver.resolve(POST, "Senior Engineer")).isEqualTo("SeniorEngineer");
        assertThat(resolver.resolve(GENDER, "Female")).isEqualTo("2");
    }

    @Test
    @DisplayName("首尾空格容忍_Excel里常见")
    void 首尾空格容忍_Excel里常见() {
        assertThat(resolver.resolve(POST, "  经理  ")).isEqualTo("Manager");
        assertThat(resolver.resolve(GENDER, " Male ")).isEqualTo("1");
    }

    @Test
    @DisplayName("空白输入返回null_按未填处理")
    void 空白输入返回null_按未填处理() {
        assertThat(resolver.resolve(POST, null)).isNull();
        assertThat(resolver.resolve(POST, "")).isNull();
        assertThat(resolver.resolve(POST, "   ")).isNull();
    }

    @Test
    @DisplayName("认不出的值返回null_由调用方决定是报错还是留空")
    void 认不出的值返回null_由调用方决定是报错还是留空() {
        assertThat(resolver.resolve(POST, "司机")).isNull();      // V13 已删掉的字典项
        assertThat(resolver.resolve(POST, "打杂的")).isNull();
        assertThat(resolver.resolve(GENDER, "9")).isNull();
    }

    @Test
    @DisplayName("码优先于label_避免结果取决于遍历顺序")
    void 码优先于label_避免结果取决于遍历顺序() {
        /*
         * 「Manager」既是码 Manager，也是 Manager 的英文 label；「Engineer」同理。
         * 这类重合在字典码用英文单词时很常见。先匹配码保证结果稳定，
         * 而不是取决于 Map 的遍历顺序。
         */
        assertThat(resolver.resolve(POST, "Engineer")).isEqualTo("Engineer");
        assertThat(resolver.resolve(POST, "Director")).isEqualTo("Director");
    }
}
