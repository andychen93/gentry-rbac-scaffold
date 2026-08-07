package com.precision.core.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类
 */
public final class IpUtil {

    private IpUtil() {}

    /**
     * 从请求中获取客户端真实 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) ip = request.getHeader("X-Real-IP");
        if (isBlank(ip)) ip = request.getRemoteAddr();
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // IPv6 回环地址转 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * 根据 IP 获取归属地（简单实现）
     */
    public static String getLocation(String ip) {
        if (ip == null) return null;
        if ("127.0.0.1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "本机";
        }
        // TODO: 接入离线 IP 库（如 ip2region）解析公网 IP 归属地
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty() || "unknown".equalsIgnoreCase(s);
    }
}
