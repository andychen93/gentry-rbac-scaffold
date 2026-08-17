package com.gentry.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class IpUtilTest {

    private MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(remoteAddr);
        return req;
    }

    @Test
    @DisplayName("getClientIp_无代理头_取remoteAddr")
    void getClientIp_noProxyHeader_usesRemoteAddr() {
        assertThat(IpUtil.getClientIp(request("203.0.113.5"))).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("getClientIp_有XFF_优先取XFF而非remoteAddr")
    void getClientIp_withXff_prefersXff() {
        MockHttpServletRequest req = request("127.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.9");
        assertThat(IpUtil.getClientIp(req)).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("getClientIp_多级代理_取最左侧客户端地址")
    void getClientIp_multipleProxies_takesLeftmost() {
        MockHttpServletRequest req = request("127.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1, 10.0.0.2");
        assertThat(IpUtil.getClientIp(req)).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("getClientIp_XFF首段为unknown_跳过占位取下一个")
    void getClientIp_xffLeadingUnknown_skipsPlaceholder() {
        // 部分代理会写 unknown 占位，原实现直接取 split[0] 会把 "unknown" 当 IP 记下来
        MockHttpServletRequest req = request("127.0.0.1");
        req.addHeader("X-Forwarded-For", "unknown, 203.0.113.9");
        assertThat(IpUtil.getClientIp(req)).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("getClientIp_XFF为unknown_回退X-Real-IP")
    void getClientIp_xffUnknown_fallsBackToRealIp() {
        MockHttpServletRequest req = request("127.0.0.1");
        req.addHeader("X-Forwarded-For", "unknown");
        req.addHeader("X-Real-IP", "198.51.100.7");
        assertThat(IpUtil.getClientIp(req)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("getClientIp_IPv6回环_归一成127.0.0.1")
    void getClientIp_ipv6Loopback_normalized() {
        assertThat(IpUtil.getClientIp(request("::1"))).isEqualTo("127.0.0.1");
        assertThat(IpUtil.getClientIp(request("0:0:0:0:0:0:0:1"))).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("getClientIp_IPv4映射的IPv6_剥成纯IPv4")
    void getClientIp_ipv4MappedIpv6_strippedToIpv4() {
        // 监听 :: 时 socket/node 代理给出的就是这种形态，不剥会导致内网段判断失配
        assertThat(IpUtil.getClientIp(request("::ffff:172.20.10.3"))).isEqualTo("172.20.10.3");

        MockHttpServletRequest req = request("127.0.0.1");
        req.addHeader("X-Forwarded-For", "::ffff:192.168.1.9");
        assertThat(IpUtil.getClientIp(req)).isEqualTo("192.168.1.9");
    }

    @Test
    @DisplayName("getClientIp_真IPv6地址_原样保留")
    void getClientIp_realIpv6_preserved() {
        assertThat(IpUtil.getClientIp(request("2001:db8::8a2e:370:7334")))
                .isEqualTo("2001:db8::8a2e:370:7334");
    }

    @Test
    @DisplayName("getLocation_IPv4映射的内网地址_返回局域网")
    void getLocation_ipv4MappedPrivate_returnsLan() {
        assertThat(IpUtil.getLocation(IpUtil.getClientIp(request("::ffff:172.20.10.3"))))
                .isEqualTo("局域网");
    }

    @Test
    @DisplayName("getLocation_回环地址_返回本机")
    void getLocation_loopback_returnsLocal() {
        assertThat(IpUtil.getLocation("127.0.0.1")).isEqualTo("本机");
    }

    @Test
    @DisplayName("getLocation_内网地址_返回局域网")
    void getLocation_privateRanges_returnLan() {
        assertThat(IpUtil.getLocation("10.1.2.3")).isEqualTo("局域网");
        assertThat(IpUtil.getLocation("192.168.1.5")).isEqualTo("局域网");
        assertThat(IpUtil.getLocation("172.16.0.1")).isEqualTo("局域网");
        assertThat(IpUtil.getLocation("172.20.10.3")).isEqualTo("局域网");
        assertThat(IpUtil.getLocation("172.31.255.254")).isEqualTo("局域网");
    }

    @Test
    @DisplayName("getLocation_172开头但非私有段_不得判为内网")
    void getLocation_172PublicRange_notTreatedAsPrivate() {
        // 回归：原实现 ip.startsWith("172.") 把这些公网地址误判成内网
        assertThat(IpUtil.getLocation("172.15.0.1")).isNotEqualTo("局域网");
        assertThat(IpUtil.getLocation("172.32.0.1")).isNotEqualTo("局域网");
        assertThat(IpUtil.getLocation("172.200.1.1")).isNotEqualTo("局域网");
    }

    @Test
    @DisplayName("getLocation_null_返回null")
    void getLocation_null_returnsNull() {
        assertThat(IpUtil.getLocation(null)).isNull();
    }
}
