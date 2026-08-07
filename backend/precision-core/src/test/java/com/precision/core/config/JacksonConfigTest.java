package com.precision.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JacksonConfig 单元测试：直接把 customizer 作用到 Jackson2ObjectMapperBuilder 上，
 * 构造测试用 ObjectMapper，验证各序列化/反序列化规则。
 */
@DisplayName("Jackson 全局配置")
class JacksonConfigTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        mapper = builder.build();
    }

    @Test
    @DisplayName("Long 序列化为 String（雪花 ID 不丢精度）")
    void longSerializedAsString() throws Exception {
        record Payload(Long id, long count) {}
        Payload p = new Payload(1905068534563287041L, 42L);
        String json = mapper.writeValueAsString(p);

        assertThat(json)
                .contains("\"id\":\"1905068534563287041\"")
                .contains("\"count\":\"42\"");
    }

    @Test
    @DisplayName("Long 反序列化仍能解析 String 与数字两种输入")
    void longDeserializedFromBothForms() throws Exception {
        record Payload(Long id) {}
        Payload fromStr = mapper.readValue("{\"id\":\"12345\"}", Payload.class);
        Payload fromNum = mapper.readValue("{\"id\":12345}", Payload.class);
        assertThat(fromStr.id()).isEqualTo(12345L);
        assertThat(fromNum.id()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("LocalDateTime 格式 yyyy-MM-dd HH:mm:ss")
    void localDateTimeFormat() throws Exception {
        record T(LocalDateTime time) {}
        T t = new T(LocalDateTime.of(2026, 5, 5, 18, 30, 45));
        String json = mapper.writeValueAsString(t);
        assertThat(json).contains("\"time\":\"2026-05-05 18:30:45\"");

        T back = mapper.readValue(json, T.class);
        assertThat(back.time()).isEqualTo(t.time());
    }

    @Test
    @DisplayName("LocalDate 格式 yyyy-MM-dd")
    void localDateFormat() throws Exception {
        record T(LocalDate d) {}
        String json = mapper.writeValueAsString(new T(LocalDate.of(2026, 5, 5)));
        assertThat(json).contains("\"d\":\"2026-05-05\"");
    }

    @Test
    @DisplayName("LocalTime 格式 HH:mm:ss")
    void localTimeFormat() throws Exception {
        record T(LocalTime t) {}
        String json = mapper.writeValueAsString(new T(LocalTime.of(9, 15, 0)));
        assertThat(json).contains("\"t\":\"09:15:00\"");
    }

    @Test
    @DisplayName("null 字段序列化时被跳过（NON_NULL）")
    void nullFieldsSkipped() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("a", "x");
        m.put("b", null);
        String json = mapper.writeValueAsString(m);
        assertThat(json).contains("\"a\":\"x\"").doesNotContain("\"b\":");
    }

    @Test
    @DisplayName("未知字段反序列化时忽略（FAIL_ON_UNKNOWN_PROPERTIES=false）")
    void unknownFieldsIgnored() throws Exception {
        record Known(String name) {}
        Known k = mapper.readValue("{\"name\":\"alice\",\"unknown\":123}", Known.class);
        assertThat(k.name()).isEqualTo("alice");
    }

    @Test
    @DisplayName("常量确保与设计规范一致")
    void constantsMatchDesign() {
        assertThat(JacksonConfig.DATE_TIME_PATTERN).isEqualTo("yyyy-MM-dd HH:mm:ss");
        assertThat(JacksonConfig.DATE_PATTERN).isEqualTo("yyyy-MM-dd");
        assertThat(JacksonConfig.TIME_PATTERN).isEqualTo("HH:mm:ss");
        assertThat(JacksonConfig.DEFAULT_TIMEZONE).isEqualTo("Asia/Shanghai");
    }
}
