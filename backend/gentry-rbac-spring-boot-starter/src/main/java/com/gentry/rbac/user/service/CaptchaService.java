package com.gentry.rbac.user.service;

import com.gentry.rbac.user.vo.CaptchaVO;

/**
 * 登录验证码服务。
 *
 * <p>验证码答案存 Redis（key={@code captcha:{uuid}}），一次性使用：校验后无论对错均删除。</p>
 */
public interface CaptchaService {

    /**
     * 生成验证码图片与唯一标识。
     *
     * @return 验证码 VO；开关关闭（sys_config {@code sys.captcha.enabled} 或 yml 兜底为 false）时返回
     *         {@code null}，前端登录页据此不渲染验证码输入框
     */
    CaptchaVO generate();

    /**
     * 校验验证码。
     *
     * @param uuid  generate() 返回的 uuid
     * @param input 用户输入的验证码答案
     * @throws com.gentry.core.exception.BizException 验证码错误或已过期（ErrorCode.CAPTCHA_ERROR）
     */
    void validate(String uuid, String input);
}
