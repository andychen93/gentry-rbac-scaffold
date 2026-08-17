package com.gentry.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 全局配置。
 *
 * <p>集中处理平台 JSON 序列化/反序列化行为，解决以下核心问题：</p>
 * <ol>
 *     <li><b>Long 精度丢失</b>：Long 类型（含 long 基本类型）序列化为 String，
 *         避免 JS {@code Number} 类型 {@code 2^53 - 1} 最大安全整数限制</li>
 *     <li><b>日期格式统一</b>：{@code LocalDateTime / LocalDate / LocalTime / java.util.Date}
 *         使用 {@code yyyy-MM-dd HH:mm:ss} / {@code yyyy-MM-dd} / {@code HH:mm:ss} 格式</li>
 *     <li><b>空值过滤</b>：{@code @JsonInclude(NON_NULL)} 跳过 null 字段</li>
 *     <li><b>时区统一</b>：{@code Asia/Shanghai}</li>
 *     <li><b>容错</b>：{@code FAIL_ON_UNKNOWN_PROPERTIES=false}，前端新增字段不报错</li>
 * </ol>
 *
 * <p>通过 {@link Jackson2ObjectMapperBuilderCustomizer} 统一覆盖 Spring Boot
 * 默认的 {@code ObjectMapper}，无需显式重建 Bean，与其他 starter（MyBatis-Flex / Sa-Token Redis）兼容。</p>
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter tf = DateTimeFormatter.ofPattern(TIME_PATTERN);
        TimeZone zone = TimeZone.getTimeZone(DEFAULT_TIMEZONE);

        return builder -> {
            // ===== Long → String：防 JS 精度丢失（雪花 ID 超过 2^53） =====
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // ===== Java 8 时间类型：统一格式化 =====
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dtf));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dtf));
            javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(df));
            javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(df));
            javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(tf));
            javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(tf));
            builder.modules(javaTimeModule);

            // ===== 全局序列化开关 =====
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.timeZone(zone);
            builder.simpleDateFormat(DATE_TIME_PATTERN);

            builder.featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.FAIL_ON_EMPTY_BEANS,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
            );
        };
    }

    /** 工具：构造与全局一致的 {@link SimpleDateFormat}（老代码偶尔需要用 Date 格式化时调用） */
    public static SimpleDateFormat dateTimeFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
        return sdf;
    }
}
