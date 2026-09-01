package com.gentry.rbac.user.service;

import com.gentry.rbac.user.dto.EmailDTO;
import com.gentry.rbac.user.dto.PasswordResetDTO;
import com.gentry.rbac.user.dto.RegisterDTO;
import com.gentry.rbac.user.dto.TokenDTO;

/**
 * 邮箱自助认证：注册（发验证邮件）→ 验证（建用户）→ 邮箱登录校验 / 找回密码。
 *
 * <p>注册与验证两段式：{@code register} 只落令牌发邮件，用户在 {@code verifyEmail}
 * 时才创建。密码 BCrypt 哈希暂存于令牌 payload，令牌一次性、用后即焚。</p>
 */
public interface AuthEmailService {

    /** AUTH-007 注册：邮箱未占用则发验证邮件（48h 一次性令牌）。邮件失败不回滚，可重发。 */
    void register(RegisterDTO dto);

    /** AUTH-008 验证邮箱：令牌有效即创建用户（username 由平台生成）并回填令牌。 */
    void verifyEmail(TokenDTO dto);

    /** AUTH-009 重发验证邮件：同邮箱冷却期内拒绝。 */
    void resendVerification(EmailDTO dto);

    /** AUTH-010 忘记密码：邮箱存在才发信，但无论是否存在都静默成功（防枚举）。 */
    void forgotPassword(EmailDTO dto);

    /** AUTH-011 重置密码：令牌有效则改密码并踢下线（黑名单 + kickout）。 */
    void resetPassword(PasswordResetDTO dto);

    /** 邮箱是否已完成验证（登录前置校验用）。 */
    boolean isEmailVerified(String email);
}
