package com.precision.core.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 请求日志过滤器注册：优先级略低于 TraceIdFilter，高于 Sa-Token 拦截器。
 */
@Configuration
public class RequestLogFilterConfig {

    @Bean
    public FilterRegistrationBean<RequestLogFilter> requestLogFilterRegistration(RequestLogProperties properties) {
        FilterRegistrationBean<RequestLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestLogFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("requestLogFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
