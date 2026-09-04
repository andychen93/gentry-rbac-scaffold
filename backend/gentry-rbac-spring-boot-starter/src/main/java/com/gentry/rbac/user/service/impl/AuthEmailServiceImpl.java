package com.gentry.rbac.user.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.TokenBlacklistService;
import com.gentry.rbac.mail.GentryMailProperties;
import com.gentry.rbac.mail.MailGateway;
import com.gentry.rbac.config.GentryRegisterProperties;
import com.gentry.rbac.role.entity.Role;
import com.gentry.rbac.role.mapper.RoleMapper;
import com.gentry.rbac.user.dto.EmailDTO;
import com.gentry.rbac.user.dto.PasswordResetDTO;
import com.gentry.rbac.user.dto.RegisterDTO;
import com.gentry.rbac.user.dto.TokenDTO;
import com.gentry.rbac.user.entity.EmailToken;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.entity.UserRole;
import com.gentry.rbac.user.mapper.EmailTokenMapper;
import com.gentry.rbac.user.mapper.UserMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import com.gentry.rbac.user.service.AuthEmailService;
import com.gentry.core.util.IdGenerator;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 邮箱自助认证实现。
 *
 * <p>安全要点：令牌 32 字节 SecureRandom、只存 SHA-256、一次性、用后置 deleted；
 * forgotPassword 防枚举（邮箱不存在也返回成功）；resetPassword 后黑名单 + kickout。</p>
 */
@Slf4j
@Service
public class AuthEmailServiceImpl implements AuthEmailService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailTokenMapper emailTokenMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailGateway mailGateway;
    private final GentryMailProperties mailProperties;
    private final GentryRegisterProperties registerProperties;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public AuthEmailServiceImpl(EmailTokenMapper emailTokenMapper,
                                UserMapper userMapper,
                                PasswordEncoder passwordEncoder,
                                MailGateway mailGateway,
                                GentryMailProperties mailProperties,
                                GentryRegisterProperties registerProperties,
                                RoleMapper roleMapper,
                                UserRoleMapper userRoleMapper) {
        this.emailTokenMapper = emailTokenMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailGateway = mailGateway;
        this.mailProperties = mailProperties;
        this.registerProperties = registerProperties;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public void register(RegisterDTO dto) {
        String email = normalize(dto.getEmail());

        // 邮箱已被已验证用户占用
        if (userMapper.countByEmail(email) > 0) {
            throw new BizException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 重发冷却：同邮箱同用途存在未使用且在冷却窗口内的令牌
        EmailToken latest = emailTokenMapper.selectLatestByEmailAndPurpose(
                email, EmailToken.PURPOSE_REGISTER);
        if (latest != null && latest.getUsedAt() == null && latest.getExpiresAt() != null
                && latest.getExpiresAt().isAfter(LocalDateTime.now())
                && latest.getCreateTime() != null
                && latest.getCreateTime().plusMinutes(mailProperties.getResendCooldownMinutes())
                        .isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        // 生成令牌：32 字节随机数 Base64URL，只落 SHA-256
        String token = generateToken();
        EmailToken emailToken = new EmailToken();
        emailToken.setEmail(email);
        emailToken.setPurpose(EmailToken.PURPOSE_REGISTER);
        emailToken.setTokenHash(sha256(token));
        emailToken.setPayload(payloadJson(passwordEncoder.encode(dto.getPassword()), dto.getNickname()));
        emailToken.setExpiresAt(LocalDateTime.now().plusHours(mailProperties.getRegisterTokenTtlHours()));
        emailTokenMapper.insert(emailToken);

        sendVerifyMail(email, token);
    }

    @Override
    @Transactional
    public void verifyEmail(TokenDTO dto) {
        EmailToken emailToken = consumeToken(dto.getToken(), EmailToken.PURPOSE_REGISTER);

        // 并发兜底：两封信同邮箱场景，第二封验证时邮箱可能已被占用
        String email = emailToken.getEmail();
        if (userMapper.countByEmail(email) > 0) {
            throw new BizException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 解出注册凭据
        String[] credential = parsePayload(emailToken.getPayload());
        String passwordHash = credential[0];
        String nickname = credential[1];

        // 创建用户：先拿 flexId 生成 username（满足 ^[a-zA-Z][a-zA-Z0-9_]{3,19}$，全局不冲突）
        Long userId = IdGenerator.nextId();
        User user = new User();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setPassword(passwordHash);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);

        // 回填令牌归属（consumeToken 已置 used_at）
        emailToken.setUserId(user.getId());
        emailTokenMapper.update(emailToken);

        // 按消费方配置绑定默认角色（如 budget 的 BUDGET_USER）；
        // 角色未配置或不存在时静默跳过，不阻断注册
        bindDefaultRole(user.getId());
    }

    @Override
    public void resendVerification(EmailDTO dto) {
        String email = normalize(dto.getEmail());

        if (userMapper.countByEmail(email) > 0) {
            // 已验证过，无需再发；静默成功避免探测注册状态
            return;
        }

        EmailToken latest = emailTokenMapper.selectLatestByEmailAndPurpose(
                email, EmailToken.PURPOSE_REGISTER);
        if (latest != null && latest.getUsedAt() == null && latest.getExpiresAt() != null
                && latest.getExpiresAt().isAfter(LocalDateTime.now())
                && latest.getCreateTime() != null
                && latest.getCreateTime().plusMinutes(mailProperties.getResendCooldownMinutes())
                        .isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        // 无历史令牌（未发起过注册）也静默成功，行为与 register 保持一致的可观测性边界
        if (latest == null) {
            return;
        }

        // 复用最近一次令牌的凭据重发新令牌（旧令牌作废：置 deleted）
        String token = generateToken();
        EmailToken fresh = new EmailToken();
        fresh.setEmail(email);
        fresh.setPurpose(EmailToken.PURPOSE_REGISTER);
        fresh.setTokenHash(sha256(token));
        fresh.setPayload(latest.getPayload());
        fresh.setExpiresAt(LocalDateTime.now().plusHours(mailProperties.getRegisterTokenTtlHours()));
        emailTokenMapper.insert(fresh);
        latest.setDeleted(1);
        emailTokenMapper.update(latest);

        sendVerifyMail(email, token);
    }

    @Override
    public void forgotPassword(EmailDTO dto) {
        String email = normalize(dto.getEmail());

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            // 防枚举：邮箱不存在也静默成功
            return;
        }

        String token = generateToken();
        EmailToken emailToken = new EmailToken();
        emailToken.setEmail(email);
        emailToken.setPurpose(EmailToken.PURPOSE_RESET_PASSWORD);
        emailToken.setTokenHash(sha256(token));
        emailToken.setExpiresAt(LocalDateTime.now().plusMinutes(mailProperties.getResetTokenTtlMinutes()));
        emailTokenMapper.insert(emailToken);

        sendResetMail(email, token);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDTO dto) {
        EmailToken emailToken = consumeToken(dto.getToken(), EmailToken.PURPOSE_RESET_PASSWORD);

        User user = userMapper.selectByEmail(emailToken.getEmail());
        if (user == null) {
            throw new BizException(ErrorCode.EMAIL_TOKEN_INVALID);
        }

        String encoded = passwordEncoder.encode(dto.getNewPassword());
        userMapper.updatePassword(user.getId(), encoded, LocalDateTime.now());

        // 重置后踢下线（对齐 UserServiceImpl.resetPassword 的做法）
        try {
            TokenBlacklistService.blacklistAllTokensOfUser(user.getId());
        } catch (Exception e) {
            log.warn("Failed to blacklist tokens of user {} after email reset: {}", user.getId(), e.getMessage());
        }
        try {
            StpUtil.kickout(user.getId());
        } catch (Exception e) {
            log.warn("Failed to kickout user {} after email reset: {}", user.getId(), e.getMessage());
        }
    }

    @Override
    public boolean isEmailVerified(String email) {
        EmailToken latest = emailTokenMapper.selectLatestByEmailAndPurpose(
                normalize(email), EmailToken.PURPOSE_REGISTER);
        return latest != null && latest.getUsedAt() != null;
    }

    // ========== 私有方法 ==========

    /** 消费方配置了默认角色编码且角色存在时，为新用户绑定；失败只记日志不抛错 */
    private void bindDefaultRole(Long userId) {
        String roleCode = registerProperties.getDefaultRoleCode();
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }
        try {
            Role role = roleMapper.selectOneByQuery(QueryWrapper.create()
                    .eq(Role::getRoleCode, roleCode)
                    .eq(Role::getDeleted, 0)
                    .limit(1));
            if (role == null) {
                log.warn("Default role [{}] not found, skip binding for user {}", roleCode, userId);
                return;
            }
            UserRole userRole = new UserRole(userId, role.getId());
            userRoleMapper.insert(userRole);
        } catch (Exception e) {
            log.warn("Failed to bind default role [{}] for user {}: {}", roleCode, userId, e.getMessage());
        }
    }

    /** 校验并消费令牌：无效/过期/已用/purpose 不符统一 EMAIL_TOKEN_INVALID，不泄露具体原因 */
    private EmailToken consumeToken(String token, String purpose) {
        EmailToken emailToken = emailTokenMapper.selectByTokenHash(sha256(token));
        if (emailToken == null
                || !purpose.equals(emailToken.getPurpose())
                || emailToken.getUsedAt() != null
                || emailToken.getExpiresAt() == null
                || emailToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.EMAIL_TOKEN_INVALID);
        }
        emailToken.setUsedAt(LocalDateTime.now());
        return emailToken;
    }

    private void sendVerifyMail(String email, String token) {
        String link = mailProperties.getVerifyUrlBase() + "/verify-email?token=" + token;
        String subject = "邮箱验证";
        String body = "请点击以下链接完成注册验证（48 小时内有效）：" + link;
        try {
            mailGateway.send(email, subject, body);
        } catch (Exception e) {
            // 邮件失败不回滚令牌：用户可走重发
            log.error("Failed to send verify mail to {}: {}", email, e.getMessage());
        }
    }

    private void sendResetMail(String email, String token) {
        String link = mailProperties.getResetUrlBase() + "/reset-password?token=" + token;
        String subject = "重置密码";
        String body = "请点击以下链接重置密码（30 分钟内有效）：" + link;
        try {
            mailGateway.send(email, subject, body);
        } catch (Exception e) {
            log.error("Failed to send reset mail to {}: {}", email, e.getMessage());
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** 极简 JSON 转义：凭据只有两个字符串字段，不引 JSON 库 */
    private static String payloadJson(String passwordHash, String nickname) {
        return "{\"p\":\"" + escape(passwordHash) + "\",\"n\":\"" + escape(nickname) + "\"}";
    }

    private static String[] parsePayload(String payload) {
        // 极简解析：{"p":"...","n":"..."}；BCrypt 哈希与昵称均不含引号/反斜杠（昵称已被
        // DTO 校验约束为 2-20 字符，但仍做转义防御）
        try {
            String p = extractJsonField(payload, "p");
            String n = extractJsonField(payload, "n");
            return new String[]{p, n};
        } catch (Exception e) {
            throw new BizException(ErrorCode.EMAIL_TOKEN_INVALID);
        }
    }

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key) + key.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                sb.append(json.charAt(++i));
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
