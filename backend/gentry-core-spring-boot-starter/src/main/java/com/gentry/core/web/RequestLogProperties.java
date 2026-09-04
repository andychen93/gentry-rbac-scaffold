package com.gentry.core.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 请求日志过滤器配置。
 *
 * <p>在 application.yml 中配置：</p>
 * <pre>
 * gentry:
 *   request-log:
 *     enabled: true
 *     slow-threshold: 1000
 *     max-body-length: 2048
 *     exclude-paths:
 *       - /actuator/**
 *       - /swagger-ui/**
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "gentry.request-log")
public class RequestLogProperties {

    /** 是否启用请求日志 */
    private boolean enabled = true;

    /** 慢请求阈值（毫秒），超过则以 WARN 级别输出 */
    private int slowThreshold = 1000;

    /** 请求/响应体最大记录长度（字节） */
    private int maxBodyLength = 2048;

    /** 需要跳过的路径（Ant 风格通配），新增配置会合并默认值 */
    private List<String> excludePaths = new ArrayList<>(Arrays.asList(
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/favicon.ico"
    ));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getSlowThreshold() { return slowThreshold; }
    public void setSlowThreshold(int slowThreshold) { this.slowThreshold = slowThreshold; }

    public int getMaxBodyLength() { return maxBodyLength; }
    public void setMaxBodyLength(int maxBodyLength) { this.maxBodyLength = maxBodyLength; }

    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
}
