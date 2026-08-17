package com.gentry.rbac.user.controller;

import com.gentry.core.common.R;
import com.gentry.rbac.user.service.CaptchaService;
import com.gentry.rbac.user.vo.CaptchaVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录验证码接口（公开，已在 SaTokenConfig 放行）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /** 获取登录验证码图片 */
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(captchaService.generate());
    }
}
