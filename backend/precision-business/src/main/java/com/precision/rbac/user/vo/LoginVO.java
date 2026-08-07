package com.precision.rbac.user.vo;

/**
 * 登录响应 VO。
 * <p>JWT 模式下 {@code token} 为 JWT 字符串（{@code eyJ...}）。</p>
 * <p>{@code refreshToken} 为 RefreshToken 预留字段，首期可为 null，后续迭代支持无感续期。</p>
 */
public class LoginVO {
    private String token;
    private String refreshToken;
    private UserInfoVO userInfo;

    public LoginVO() {}

    public LoginVO(String token, UserInfoVO userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }

    public LoginVO(String token, String refreshToken, UserInfoVO userInfo) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userInfo = userInfo;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserInfoVO getUserInfo() { return userInfo; }
    public void setUserInfo(UserInfoVO userInfo) { this.userInfo = userInfo; }
}
