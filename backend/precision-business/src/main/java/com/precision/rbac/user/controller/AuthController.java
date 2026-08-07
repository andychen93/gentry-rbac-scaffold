package com.precision.rbac.user.controller;

import com.precision.core.common.R;
import com.precision.core.util.IpUtil;
import com.precision.core.web.RateLimit;
import com.precision.core.web.RateLimitKeyType;
import com.precision.core.web.RepeatSubmit;
import com.precision.rbac.user.dto.LoginDTO;
import com.precision.rbac.user.dto.UserPasswordUpdateDTO;
import com.precision.rbac.user.service.AuthService;
import com.precision.rbac.user.service.UserService;
import com.precision.rbac.user.vo.LoginVO;
import com.precision.rbac.user.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /** AUTH-001 用户登录（按 IP 限流：每分钟 10 次，防暴力破解） */
    @PostMapping("/login")
    @RateLimit(keyType = RateLimitKeyType.IP, count = 10, period = 60, message = "登录尝试过于频繁，请稍后重试")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String loginIp = IpUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        return R.ok(authService.login(dto, loginIp, userAgent));
    }

    /** AUTH-002 用户登出 */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /** AUTH-003 获取用户信息 */
    @GetMapping("/user-info")
    public R<UserInfoVO> getUserInfo() {
        return R.ok(authService.getUserInfo());
    }

    /** AUTH-004 修改密码（防重复提交：5 秒内禁止重复） */
    @PutMapping("/password")
    @RepeatSubmit(interval = 5, message = "密码修改请求过快")
    public R<Void> updatePassword(@Valid @RequestBody UserPasswordUpdateDTO dto) {
        userService.updatePassword(dto);
        return R.ok();
    }
}
