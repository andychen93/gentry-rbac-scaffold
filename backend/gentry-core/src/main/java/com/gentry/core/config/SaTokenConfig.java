package com.gentry.core.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.gentry.core.security.TokenBlacklistService;
import com.gentry.core.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. Sa-Token 登录校验 + JWT 黑名单校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
            // JWT 模式下：额外检查 Token 是否已加入黑名单（登出/改密码/强制下线）
            String tokenValue = StpUtil.getTokenValue();
            if (tokenValue != null && TokenBlacklistService.isBlacklisted(tokenValue)) {
                // 清理当前会话并抛出未登录异常，让全局异常处理器返回 401
                try {
                    StpUtil.logout();
                } catch (Exception ignored) {
                }
                throw NotLoginException.newInstance(StpUtil.getLoginType(),
                        NotLoginException.TOKEN_TIMEOUT, "Token 已被吊销", tokenValue);
            }
        }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/captcha",
                        "/api/v1/tenants/options",
                        /*
                         * SSE 通知流：浏览器 EventSource 不能自定义请求头，token 只能走 query 参数，
                         * 拦截器读不到头会直接 401。故在此放行，改由
                         * NotificationSseController 自己用 StpUtil.getLoginIdByToken 手动校验。
                         */
                        "/api/v1/notifications/stream",
                        "/actuator/**"
                );

        // 2. 从 Sa-Token Session 恢复 UserContext（登录校验通过后执行）
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (StpUtil.isLogin()) {
                    SaSession session = StpUtil.getSession();
                    Object userId = session.get("userId");
                    Object tenantId = session.get("tenantId");
                    Object deptId = session.get("deptId");
                    Object platformAdmin = session.get("platformAdmin");
                    if (userId instanceof Number) UserContext.setUserId(((Number) userId).longValue());
                    if (tenantId instanceof Number) UserContext.setTenantId(((Number) tenantId).longValue());
                    if (deptId instanceof Number) UserContext.setDeptId(((Number) deptId).longValue());
                    UserContext.setPlatformAdmin(Boolean.TRUE.equals(platformAdmin));
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                UserContext.clear();
            }
        }).addPathPatterns("/api/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
