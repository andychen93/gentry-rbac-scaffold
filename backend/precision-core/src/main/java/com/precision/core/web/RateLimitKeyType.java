package com.precision.core.web;

/**
 * 限流维度。
 */
public enum RateLimitKeyType {
    /** 按客户端 IP 限流 */
    IP,
    /** 按当前登录用户 ID 限流（未登录退化为 IP） */
    USER_ID,
    /** 全局限流（不区分用户/IP） */
    GLOBAL
}
