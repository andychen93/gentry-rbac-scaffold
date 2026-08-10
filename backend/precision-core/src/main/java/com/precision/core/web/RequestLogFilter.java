package com.precision.core.web;

import com.precision.core.security.UserContext;
import com.precision.core.trace.TraceContext;
import com.precision.core.util.IpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 请求日志过滤器，对每个 HTTP 请求输出结构化日志（traceId / userId / IP / method / path / status / duration）。
 *
 * <p>设计要点：</p>
 * <ul>
 *     <li>继承 {@link OncePerRequestFilter} 保证一次请求仅执行一次（Async 转发、forward 也不重复打印）</li>
 *     <li>使用 {@link ContentCachingRequestWrapper}/{@link ContentCachingResponseWrapper} 支持读取请求/响应体</li>
 *     <li>仅对 POST/PUT/PATCH 打印 body；登录 / 改密码 等敏感路径做字段脱敏</li>
 *     <li>根据状态码与耗时自动选择日志级别：5xx → ERROR，4xx / slow → WARN，其他 → INFO</li>
 * </ul>
 */
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 敏感路径 → 敏感字段清单（JSON 字段名），路径支持 Ant 通配 */
    private static final Map<String, List<String>> SENSITIVE_PATHS = Map.of(
            "/api/v1/auth/login", List.of("password", "captcha"),
            "/api/v1/auth/password", List.of("oldPassword", "newPassword"),
            // 管理员重置密码：PUT /api/v1/users/{id}/password/reset
            "/api/v1/users/*/password/reset", List.of("newPassword")
    );

    private static final String MASK = "***";

    private final RequestLogProperties properties;

    public RequestLogFilter(RequestLogProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // SSE / 流式响应不能被 ContentCachingResponseWrapper 缓冲（会阻断实时推送），直接放行
        if (!properties.isEnabled() || shouldSkip(request.getRequestURI()) || isStreaming(request)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedReq = wrapRequest(request);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);
        long start = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedReq, wrappedResp);
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                logEntry(wrappedReq, wrappedResp, duration);
            } catch (Exception e) {
                log.warn("请求日志输出失败: {}", e.getMessage());
            }
            // 必须回写响应体，否则客户端收不到响应
            wrappedResp.copyBodyToResponse();
        }
    }

    /** 判断是否为 SSE / 流式请求（按 Accept 头或 URI 后缀），此类请求不可缓冲。 */
    private static boolean isStreaming(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/event-stream")) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && (uri.endsWith("/stream") || uri.contains("/stream"));
    }

    // ==================== 日志输出 ====================

    private void logEntry(ContentCachingRequestWrapper req, ContentCachingResponseWrapper resp, long duration) {
        String traceId = nullToDash(TraceContext.getTraceId());
        String userId = nullToDash(idOrNull(UserContext.getUserId()));
        String ip = IpUtil.getClientIp(req);
        int status = resp.getStatus();
        String method = req.getMethod();
        String path = req.getRequestURI();

        StringBuilder line = new StringBuilder(256);
        line.append('[').append(traceId).append("][user=").append(userId).append("][ip=").append(ip).append("] ")
                .append(method).append(' ').append(path)
                .append(" → ").append(status).append(" (").append(duration).append("ms)");

        if (hasBody(method)) {
            String body = bodyAsString(req);
            if (!body.isEmpty()) {
                body = maskSensitiveBody(path, body);
                body = truncate(body, properties.getMaxBodyLength());
                line.append(" body=").append(body);
            }
        }

        String msg = line.toString();
        if (status >= 500) {
            log.error(msg);
        } else if (status >= 400 || duration > properties.getSlowThreshold()) {
            log.warn(msg);
        } else {
            log.info(msg);
        }
    }

    // ==================== helpers ====================

    boolean shouldSkip(String path) {
        List<String> excludes = properties.getExcludePaths();
        if (excludes == null || excludes.isEmpty()) return false;
        for (String pattern : excludes) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    private static boolean hasBody(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private static ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper w) return w;
        return new ContentCachingRequestWrapper(request);
    }

    private static String bodyAsString(ContentCachingRequestWrapper req) {
        byte[] buf = req.getContentAsByteArray();
        if (buf.length == 0) return "";
        return new String(buf, StandardCharsets.UTF_8);
    }

    /** 敏感字段脱敏（只对已知敏感路径生效） */
    String maskSensitiveBody(String path, String body) {
        List<String> fields = null;
        for (var e : SENSITIVE_PATHS.entrySet()) {
            if (PATH_MATCHER.match(e.getKey(), path)) {
                fields = e.getValue();
                break;
            }
        }
        if (fields == null) return body;

        String masked = body;
        for (String f : fields) {
            Pattern p = Pattern.compile("(\"" + Pattern.quote(f) + "\"\\s*:\\s*)\"[^\"]*\"");
            masked = p.matcher(masked).replaceAll("$1\"" + MASK + "\"");
        }
        return masked;
    }

    static String truncate(String text, int max) {
        if (text == null) return "";
        if (max <= 0 || text.length() <= max) return text;
        return text.substring(0, max) + "...(truncated:" + text.length() + ")";
    }

    private static String nullToDash(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }

    private static String idOrNull(Long id) {
        return id == null ? null : id.toString();
    }
}
