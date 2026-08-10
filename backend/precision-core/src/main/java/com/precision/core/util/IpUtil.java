package com.precision.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * IP 工具类。
 *
 * <p>{@link #getLocation(String)} 使用 ip2region 离线库解析公网 IP 归属地。
 * xdb 文件从 classpath:ip2region/ip2region.xdb 一次性加载到内存（{@code Searcher.newWithBuffer}，
 * 线程安全，4 处调用点无需改动）；文件缺失时降级返回 null，不影响主流程。</p>
 */
public final class IpUtil {

    private static final Logger log = LoggerFactory.getLogger(IpUtil.class);

    /** 静态 holder 单例：类加载时一次性读入 xdb，查询走内存 */
    private static final Searcher SEARCHER = loadSearcher();

    private static Searcher loadSearcher() {
        try (InputStream in = IpUtil.class.getClassLoader().getResourceAsStream("ip2region/ip2region.xdb")) {
            if (in == null) {
                log.warn("ip2region/ip2region.xdb 未找到，公网 IP 归属地解析将返回 null");
                return null;
            }
            return Searcher.newWithBuffer(in.readAllBytes());
        } catch (Exception e) {
            log.warn("加载 ip2region.xdb 失败，IP 归属地解析不可用: {}", e.getMessage());
            return null;
        }
    }

    private IpUtil() {}

    /** 从请求中获取客户端真实 IP */
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

    /** 根据 IP 获取归属地 */
    public static String getLocation(String ip) {
        if (ip == null) return null;
        if ("127.0.0.1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "本机";
        }
        if (SEARCHER == null) return null;
        try {
            // 返回格式：国家|区域|省份|城市|ISP
            return SEARCHER.search(ip);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty() || "unknown".equalsIgnoreCase(s);
    }
}
