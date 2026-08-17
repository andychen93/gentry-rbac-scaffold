package com.gentry.rbac.user.controller;

import com.gentry.core.common.R;
import com.gentry.core.security.UserContext;
import com.gentry.core.util.IpUtil;
import com.gentry.core.web.RateLimit;
import com.gentry.core.web.RateLimitKeyType;
import com.gentry.core.web.RepeatSubmit;
import com.gentry.rbac.log.annotation.Log;
import com.gentry.rbac.user.dto.LoginDTO;
import com.gentry.rbac.user.dto.UserPasswordUpdateDTO;
import com.gentry.rbac.user.dto.UserProfileUpdateDTO;
import com.gentry.rbac.user.service.AuthService;
import com.gentry.rbac.user.service.UserService;
import com.gentry.rbac.user.vo.LoginVO;
import com.gentry.rbac.user.vo.UserDetailVO;
import com.gentry.rbac.user.vo.UserInfoVO;
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

    /** AUTH-005 修改个人资料（当前登录用户） */
    @PutMapping("/profile")
    @RepeatSubmit(interval = 3, message = "操作过快")
    @Log(module = "个人中心", type = "UPDATE", title = "修改个人资料")
    public R<Void> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        userService.updateProfile(dto);
        return R.ok();
    }

    /** AUTH-006 获取个人资料（完整，含手机/邮箱/职务等；仅需登录，无需 system:user:list 权限） */
    @GetMapping("/profile")
    public R<UserDetailVO> getProfile() {
        return R.ok(userService.getDetail(UserContext.getUserId()));
    }
}
