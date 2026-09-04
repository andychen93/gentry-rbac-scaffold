package com.gentry.core.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.gentry.core.i18n.GentryLocaleResolver;
import com.gentry.core.security.TokenBlacklistService;
import com.gentry.core.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** gentry.i18n.enabled=false 时该 bean 不存在，故用 ObjectProvider 可选注入 */
    private final ObjectProvider<GentryLocaleResolver> localeResolverProvider;

    public SaTokenConfig(ObjectProvider<GentryLocaleResolver> localeResolverProvider) {
        this.localeResolverProvider = localeResolverProvider;
    }

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
                        // 邮箱自助认证流（注册→验证→登录 / 找回→重置）：匿名语义，
                        // 防刷靠端点上的 @RateLimit(IP) + @RepeatSubmit，不靠登录态
                        "/api/v1/auth/register",
                        "/api/v1/auth/verify-email",
                        "/api/v1/auth/resend-verification",
                        "/api/v1/auth/password/forgot",
                        "/api/v1/auth/password/reset",
                        // 语言列表：登录页就要渲染语言选择器，必须放行。
                        // 只加 @RestController 不放行是不够的 —— 本拦截器覆盖 /api/**，
                        // 未登录访问会拿到 401（实测踩过）。
                        "/api/v1/i18n/locales",
                        /*
                         * SSE 通知流：浏览器 EventSource 不能自定义请求头，token 只能走 query 参数，
                         * 拦截器读不到头会直接 401。故在此放行，改由
                         * NotificationSseController 自己用 StpUtil.getLoginIdByToken 手动校验。
                         */
                        "/api/v1/notifications/stream",
                        "/actuator/**"
                );

        // 2. 从 Sa-Token Session 恢复 UserContext（登录校验通过后执行）+ 定稿请求语言
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (StpUtil.isLogin()) {
                    SaSession session = StpUtil.getSession();
                    Object userId = session.get("userId");
                    Object deptId = session.get("deptId");
                    Object language = session.get("language");
                    if (userId instanceof Number) UserContext.setUserId(((Number) userId).longValue());
                    if (deptId instanceof Number) UserContext.setDeptId(((Number) deptId).longValue());
                    // language 为 null 表示用户从未选过 → 下面会退到 Accept-Language
                    if (language instanceof String s) UserContext.setLanguage(s);
                }
                /*
                 * 语言在此一次性定稿，而不是交给 LocaleResolver 自己被 DispatcherServlet 调用。
                 *
                 * 原因是时序：DispatcherServlet.buildLocaleContext 对 LocaleResolver 返回的是
                 * 懒求值 lambda（每次 getLocale() 才执行），靠这个懒求值本拦截器写的 UserContext
                 * 才「恰好」已经就位。但那意味着每次 getMessage() 都要重跑一遍 Accept-Language
                 * 解析，且一旦有人把 GentryLocaleResolver 改成实现 LocaleContextResolver，
                 * 求值会变成饿的、发生在本拦截器之前，sys_user.language 将静默失效。
                 * 显式设置把解析点固定在这里：一次求值、时序明确、不依赖 Spring 内部实现细节。
                 *
                 * LocaleContextHolder 的 ThreadLocal 由 FrameworkServlet.processRequest 在
                 * finally 里 resetContextHolders(...) 恢复，无需在 afterCompletion 手动清。
                 */
                GentryLocaleResolver resolver = localeResolverProvider.getIfAvailable();
                if (resolver != null) {
                    LocaleContextHolder.setLocale(resolver.resolve(UserContext.getLanguage(), request));
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
