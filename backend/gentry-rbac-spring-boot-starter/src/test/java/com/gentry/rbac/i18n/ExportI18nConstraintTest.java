package com.gentry.rbac.i18n;

import com.alibaba.excel.annotation.ExcelProperty;
import com.gentry.rbac.user.dto.UserImportDTO;
import com.gentry.rbac.user.vo.UserExportVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导出/导入的 i18n 约束型测试（C-04）。
 *
 * <p>放在 business 而不是 core：导出 VO 在 business，core 不依赖 business，
 * 放 core 里只能靠 {@code Class.forName} + 捕获异常跳过，那是个永远不会真正执行的假测试。</p>
 */
class ExportI18nConstraintTest {

    private Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("i18n/" + name)) {
            assertThat(in).as("资源文件缺失: i18n/%s（core 的资源应在 classpath 上）", name).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    /** 取类上 @ExcelProperty(index) 的最大值 */
    private int maxColumnIndex(Class<?> clazz) {
        int max = -1;
        for (Field f : clazz.getDeclaredFields()) {
            ExcelProperty a = f.getAnnotation(ExcelProperty.class);
            if (a != null) {
                for (int idx : new int[]{a.index()}) {
                    max = Math.max(max, idx);
                }
            }
        }
        return max;
    }

    @Test
    @DisplayName("HEAD_KEYS长度与ExcelProperty列数严格一致")
    void HEAD_KEYS长度与ExcelProperty列数严格一致() {
        record Target(String name, Class<?> clazz, List<String> keys) {
        }
        List<Target> targets = List.of(
                new Target("UserExportVO", UserExportVO.class, UserExportVO.HEAD_KEYS),
                new Target("UserImportDTO", UserImportDTO.class, UserImportDTO.HEAD_KEYS));

        for (Target t : targets) {
            int max = maxColumnIndex(t.clazz());
            assertThat(max).as("%s 上没有带 index 的 @ExcelProperty", t.name()).isNotNegative();
            assertThat(t.keys().size())
                    .as("%s: HEAD_KEYS 有 %d 条但列序最大到 %d —— 加字段忘加 key 会让表头与数据列错位，"
                            + "不报错、只是每列标题挪一格，极难发现", t.name(), t.keys().size(), max)
                    .isEqualTo(max + 1);
        }
    }

    @Test
    @DisplayName("所有表头key在中英文资源文件中都存在")
    void 所有表头key在中英文资源文件中都存在() throws IOException {
        Properties zh = load("export_zh_CN.properties");
        Properties en = load("export_en_US.properties");
        for (String k : UserExportVO.HEAD_KEYS) {
            assertThat(zh.containsKey(k)).as("%s 缺 zh 译文", k).isTrue();
            assertThat(en.containsKey(k)).as("%s 缺 en 译文", k).isTrue();
        }
        for (String k : UserImportDTO.HEAD_KEYS) {
            assertThat(zh.containsKey(k)).as("%s 缺 zh 译文", k).isTrue();
            assertThat(en.containsKey(k)).as("%s 缺 en 译文", k).isTrue();
        }
    }

    @Test
    @DisplayName("导出VO与导入DTO不得残留中文ExcelProperty文案")
    void 导出VO与导入DTO不得残留中文ExcelProperty文案() {
        for (Class<?> clazz : List.of(UserExportVO.class, UserImportDTO.class)) {
            for (Field f : clazz.getDeclaredFields()) {
                ExcelProperty a = f.getAnnotation(ExcelProperty.class);
                if (a == null) {
                    continue;
                }
                for (String v : a.value()) {
                    assertThat(v)
                            .as("%s.%s 的 @ExcelProperty 还带文案 \"%s\" —— 注解常量运行时翻不了，"
                                    + "文案必须交给 HEAD_KEYS + 运行时构建表头",
                                    clazz.getSimpleName(), f.getName(), v)
                            .isBlank();
                }
            }
        }
    }

    @Test
    @DisplayName("导入DTO按列序匹配_不依赖表头文本")
    void 导入DTO按列序匹配_不依赖表头文本() {
        // 这条锁住「英文导出的 Excel 能导回来」这个不变式：
        // 一旦有人把 @ExcelProperty(index=0) 改回 @ExcelProperty("用户名")，
        // 列匹配就变回依赖表头文本，英文文件导入会静默得到全 null。
        for (Field f : UserImportDTO.class.getDeclaredFields()) {
            ExcelProperty a = f.getAnnotation(ExcelProperty.class);
            if (a == null) {
                continue;
            }
            assertThat(a.index())
                    .as("UserImportDTO.%s 必须用 index 绑定列", f.getName())
                    .isNotNegative();
        }
    }

    @Test
    @DisplayName("导出表头与前端表格表头译文一致_有意的双份维护")
    void 导出表头与前端表格表头译文一致_有意的双份维护() throws IOException {
        // 导出表头（export.user.*）与前端表格表头（user:table.*）语义重复，
        // 是本方案唯一有意接受的双份维护。前端语言包还没建，这里先只校验英文表头
        // 用的是规范的名词形式，避免出现「Please enter ...」这类表单提示混进表头。
        Properties en = load("export_en_US.properties");
        for (String k : UserExportVO.HEAD_KEYS) {
            String v = en.getProperty(k);
            assertThat(v).as("%s 无译文", k).isNotBlank();
            assertThat(v)
                    .as("%s = \"%s\" 看起来像提示句而不是表头", k, v)
                    .doesNotContain("Please").doesNotEndWith(".");
        }
    }
}
