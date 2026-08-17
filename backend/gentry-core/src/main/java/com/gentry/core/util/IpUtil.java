package com.gentry.core.util;

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

    /**
     * 从请求中获取客户端真实 IP。
     *
     * <p>取值顺序：X-Forwarded-For → X-Real-IP → {@code getRemoteAddr()}。
     * 反向代理（nginx / dev server 的 xfwd）必须透传 XFF，否则这里只能拿到代理自己的地址。</p>
     *
     * <p><b>注意</b>：XFF 是客户端可伪造的请求头，这里未做可信代理校验。
     * 它同时被 {@code @RateLimit(IP)} 用作限流键，暴露在公网时应在最外层
     * 反向代理处强制重写 XFF，而不是追加。</p>
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = firstValid(request.getHeader("X-Forwarded-For"));
        if (isBlank(ip)) ip = firstValid(request.getHeader("X-Real-IP"));
        if (isBlank(ip)) ip = request.getRemoteAddr();
        return normalize(ip);
    }

    /**
     * 取 XFF 链里第一个有效地址。
     *
     * <p>XFF 形如 {@code client, proxy1, proxy2}，最左是客户端。但部分代理会写入
     * {@code unknown} 占位（如 {@code unknown, 203.0.113.9}），
     * 原实现直接取 split[0] 会把 "unknown" 当成 IP 记下来，故逐段跳过无效值。</p>
     */
    private static String firstValid(String header) {
        if (isBlank(header)) return null;
        for (String part : header.split(",")) {
            String candidate = part.trim();
            if (!isBlank(candidate)) return candidate;
        }
        return null;
    }

    /**
     * 归一化成 IPv4 写法，避免同一来源在日志里出现多种形态、且能被内网段判断识别。
     *
     * <p>两种情况：</p>
     * <ul>
     *   <li>IPv6 回环 {@code ::1} / {@code 0:0:0:0:0:0:0:1} → {@code 127.0.0.1}</li>
     *   <li>IPv4-mapped IPv6 {@code ::ffff:172.20.10.3} → {@code 172.20.10.3}。
     *       服务端（或 node 代理）监听 {@code ::} 时，socket 拿到的就是这种形态，
     *       不剥掉的话内网段判断会失配，归属地变成 null。</li>
     * </ul>
     */
    private static String normalize(String ip) {
        if (ip == null) return null;
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
        // ::ffff:a.b.c.d（不区分大小写），只在确实是 IPv4 点分形式时剥离
        int idx = ip.lastIndexOf(':');
        if (idx > 0 && ip.regionMatches(true, 0, "::ffff:", 0, 7)) {
            String tail = ip.substring(idx + 1);
            if (tail.indexOf('.') > 0) return tail;
        }
        return ip;
    }

    /** 根据 IP 获取归属地：回环=本机，内网=局域网，公网走 ip2region */
    public static String getLocation(String ip) {
        if (ip == null) return null;
        if ("127.0.0.1".equals(ip)) return "本机";
        if (isPrivate(ip)) return "局域网";
        if (SEARCHER == null) return null;
        try {
            // 返回格式：国家|区域|省份|城市|ISP
            return SEARCHER.search(ip);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否 RFC1918 内网地址 / 链路本地地址。
     *
     * <p>原实现用 {@code ip.startsWith("172.")} 判内网，会把 172.200.1.1 这类
     * <b>公网</b>地址误判成内网（私有段只有 172.16.0.0/12，即 172.16~172.31），
     * 导致公网 IP 的归属地被写成"本机"。</p>
     */
    private static boolean isPrivate(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("169.254.")) {
            return true;
        }
        if (!ip.startsWith("172.")) return false;
        // 172.16.0.0 ~ 172.31.255.255
        int start = 4;
        int end = ip.indexOf('.', start);
        if (end < 0) return false;
        try {
            int second = Integer.parseInt(ip.substring(start, end));
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty() || "unknown".equalsIgnoreCase(s);
    }
}
