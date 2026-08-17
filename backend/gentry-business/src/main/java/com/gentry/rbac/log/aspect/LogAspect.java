package com.gentry.rbac.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentry.core.security.UserContext;
import com.gentry.core.util.IpUtil;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.log.entity.OperLog;
import com.gentry.rbac.log.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "oldPassword", "newPassword");
    private final LogService logService;
    private final ObjectMapper objectMapper;

    public LogAspect(LogService logService, ObjectMapper objectMapper) {
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperLog operLog = new OperLog();
        operLog.setModule(logAnnotation.module());
        operLog.setType(logAnnotation.type());
        operLog.setTitle(logAnnotation.title());
        operLog.setOperateTime(LocalDateTime.now());

        try {
            operLog.setOperatorId(UserContext.getUserId());
            operLog.setTenantId(UserContext.getTenantId());
        } catch (Exception ignored) {}

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = IpUtil.getClientIp(request);
                operLog.setOperatorIp(ip);
                operLog.setLocation(IpUtil.getLocation(ip));
                operLog.setMethod(request.getMethod());
                operLog.setRequestUrl(request.getRequestURI());
                // 操作员名称从 UserContext/Session 获取，而非 HTTP Header
                try {
                    cn.dev33.satoken.session.SaSession session = cn.dev33.satoken.stp.StpUtil.getSession(false);
                    if (session != null) {
                        operLog.setOperator((String) session.get("username"));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            String params = objectMapper.writeValueAsString(sanitizeParams(joinPoint.getArgs()));
            operLog.setRequestParams(params.length() > 2000 ? params.substring(0, 2000) : params);
        } catch (Exception ignored) {}

        Object result;
        try {
            result = joinPoint.proceed();
            operLog.setStatus(1);
            if (logAnnotation.saveResult() && result != null) {
                try {
                    String res = objectMapper.writeValueAsString(result);
                    operLog.setResponseResult(res.length() > 2000 ? res.substring(0, 2000) : res);
                } catch (Exception ignored) {}
            }
        } catch (Throwable ex) {
            operLog.setStatus(0);
            operLog.setErrorMsg(ex.getMessage() != null ? (ex.getMessage().length() > 2000 ? ex.getMessage().substring(0, 2000) : ex.getMessage()) : "Unknown error");
            throw ex;
        } finally {
            operLog.setCostTime((int) (System.currentTimeMillis() - startTime));
            try { logService.saveOperLog(operLog); } catch (Exception e) { log.error("Save oper log failed", e); }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object[] sanitizeParams(Object[] args) {
        if (args == null || args.length == 0) return args;
        Object[] sanitized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                sanitized[i] = null;
                continue;
            }
            try {
                // Convert to map, mask sensitive fields, convert back
                String json = objectMapper.writeValueAsString(arg);
                if (json.contains("password") || json.contains("Password")) {
                    Map<String, Object> map = objectMapper.readValue(json, Map.class);
                    for (String key : SENSITIVE_FIELDS) {
                        if (map.containsKey(key)) {
                            map.put(key, "******");
                        }
                    }
                    sanitized[i] = map;
                } else {
                    sanitized[i] = arg;
                }
            } catch (Exception e) {
                sanitized[i] = arg;
            }
        }
        return sanitized;
    }
}
