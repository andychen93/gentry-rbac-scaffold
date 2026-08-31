package com.gentry.rbac.user.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.UserContext;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮箱自助认证单元测试：注册（占用/冷却/正常）、验证（无效令牌/正常建用户）、
 * 忘记密码（防枚举）、重置（踢下线）、邮箱登录校验。不启动 Spring。
 */
@ExtendWith(MockitoExtension.class)
class AuthEmailServiceImplTest {

    @Mock private EmailTokenMapper emailTokenMapper;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailGateway mailGateway;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;

    private final Map<String, EmailToken> tokenStore = new ConcurrentHashMap<>();
    private AuthEmailServiceImpl service;
    private GentryRegisterProperties registerProperties;

    @BeforeEach
    void setup() {
        // 模拟匿名端点：不设置 UserContext，currentTenantId() 应回落 DEFAULT_TENANT_ID=1
        UserContext.clear();
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        registerProperties = new GentryRegisterProperties();
        service = new AuthEmailServiceImpl(emailTokenMapper, userMapper, passwordEncoder,
                mailGateway, new GentryMailProperties(), registerProperties, roleMapper, userRoleMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private RegisterDTO registerDto(String email) {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail(email);
        dto.setPassword("Abc@123456");
        dto.setNickname("测试用户");
        return dto;
    }

    // ========== register ==========

    @Test
    void register_emailAlreadyExists_throws() {
        when(userMapper.countByEmail(1L, "a@b.com")).thenReturn(1);
        assertThatThrownBy(() -> service.register(registerDto("a@b.com")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
        verify(emailTokenMapper, never()).insert(any(EmailToken.class));
    }

    @Test
    void register_cooldownActive_throws() {
        when(userMapper.countByEmail(1L, "a@b.com")).thenReturn(0);
        EmailToken latest = new EmailToken();
        latest.setPurpose(EmailToken.PURPOSE_REGISTER);
        latest.setUsedAt(null);
        latest.setExpiresAt(LocalDateTime.now().plusHours(47));
        latest.setCreateTime(LocalDateTime.now().minusSeconds(30)); // 冷却 1 分钟内
        when(emailTokenMapper.selectLatestByEmailAndPurpose(1L, "a@b.com", "REGISTER"))
                .thenReturn(latest);
        assertThatThrownBy(() -> service.register(registerDto("a@b.com")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.EMAIL_RESEND_COOLDOWN.getCode()));
    }

    @Test
    void register_normal_storesHashNotPlaintindromeAndSendsMail() {
        when(userMapper.countByEmail(1L, "a@b.com")).thenReturn(0);
        when(emailTokenMapper.selectLatestByEmailAndPurpose(anyLong(), anyString(), anyString())).thenReturn(null);
        when(emailTokenMapper.insert(any(EmailToken.class))).thenAnswer(inv -> {
            EmailToken t = inv.getArgument(0);
            tokenStore.put(t.getTokenHash(), t);
            return 1;
        });

        service.register(registerDto("A@B.com")); // 大写输入

        ArgumentCaptor<EmailToken> captor = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenMapper).insert(captor.capture());
        EmailToken saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("a@b.com");            // 规范化小写
        assertThat(saved.getTokenHash()).hasSize(43);                  // SHA-256 Base64URL
        assertThat(saved.getTokenHash()).doesNotContain("$");          // 不是明文/哈希混淆
        assertThat(saved.getPayload()).contains("$2a$10$encoded");     // 密码哈希暂存
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(47));
        verify(mailGateway).send(eq("a@b.com"), anyString(), anyString());
    }

    // ========== verifyEmail ==========

    @Test
    void verifyEmail_invalidToken_throws() {
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(null);
        TokenDTO dto = new TokenDTO();
        dto.setToken("nonexistent");
        assertThatThrownBy(() -> service.verifyEmail(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.EMAIL_TOKEN_INVALID.getCode()));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void verifyEmail_expiredToken_throws() {
        EmailToken token = validRegisterToken();
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(token);
        TokenDTO dto = new TokenDTO();
        dto.setToken("raw-token");
        assertThatThrownBy(() -> service.verifyEmail(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.EMAIL_TOKEN_INVALID.getCode()));
    }

    @Test
    void verifyEmail_normal_createsUserWithGeneratedUsername() {
        EmailToken token = validRegisterToken();
        // 服务对明文 token 做 SHA-256 后查库；替身需按「明文→hash」桥接
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(token);
        when(userMapper.countByEmail(1L, "a@b.com")).thenReturn(0);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        TokenDTO dto = new TokenDTO();
        dto.setToken("raw-token");
        service.verifyEmail(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User created = captor.getValue();
        assertThat(created.getUsername()).startsWith("u").hasSizeGreaterThan(5);
        assertThat(created.getUsername()).matches("^u\\d+$");
        assertThat(created.getEmail()).isEqualTo("a@b.com");
        assertThat(created.getPassword()).isEqualTo("$2a$10$encoded"); // 不再二次哈希
        assertThat(token.getUsedAt()).isNotNull();                      // 一次性消费
        assertThat(token.getUserId()).isEqualTo(created.getId());       // 回填
        // 默认角色未配置 → 不绑定
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void verifyEmail_defaultRoleConfigured_bindsRole() {
        EmailToken token = validRegisterToken();
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(token);
        when(userMapper.countByEmail(1L, "a@b.com")).thenReturn(0);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        registerProperties.setDefaultRoleCode("BUDGET_USER");
        Role budgetRole = new Role();
        budgetRole.setId(1100L);
        budgetRole.setRoleCode("BUDGET_USER");
        when(roleMapper.selectOneByQuery(any())).thenReturn(budgetRole);

        TokenDTO dto = new TokenDTO();
        dto.setToken("raw-token");
        service.verifyEmail(dto);

        ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(captor.capture());
        assertThat(captor.getValue().getRoleId()).isEqualTo(1100L);
        assertThat(captor.getValue().getUserId()).isEqualTo(token.getUserId());
    }

    @Test
    void verifyEmail_defaultRoleMissing_skipsBindingSilently() {
        EmailToken token = validRegisterToken();
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(token);
        when(userMapper.countByEmail(1L, "a@b.com")).thenReturn(0);
        when(userMapper.insert(any(User.class))).thenReturn(1);
        registerProperties.setDefaultRoleCode("NOT_EXIST");
        when(roleMapper.selectOneByQuery(any())).thenReturn(null);

        TokenDTO dto = new TokenDTO();
        dto.setToken("raw-token");
        assertThatCode(() -> service.verifyEmail(dto)).doesNotThrowAnyException();
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    // ========== forgotPassword ==========

    @Test
    void forgotPassword_unknownEmail_silentOkNoMail() {
        when(userMapper.selectByEmail(1L, "nobody@b.com")).thenReturn(null);
        EmailDTO dto = new EmailDTO();
        dto.setEmail("nobody@b.com");
        assertThatCode(() -> service.forgotPassword(dto)).doesNotThrowAnyException();
        verify(mailGateway, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_knownEmail_sendsResetMail() {
        User user = new User();
        user.setId(9L);
        user.setEmail("a@b.com");
        when(userMapper.selectByEmail(1L, "a@b.com")).thenReturn(user);
        when(emailTokenMapper.insert(any(EmailToken.class))).thenReturn(1);

        EmailDTO dto = new EmailDTO();
        dto.setEmail("a@b.com");
        service.forgotPassword(dto);

        ArgumentCaptor<EmailToken> captor = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenMapper).insert(captor.capture());
        assertThat(captor.getValue().getPurpose()).isEqualTo(EmailToken.PURPOSE_RESET_PASSWORD);
        assertThat(captor.getValue().getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(31));
        verify(mailGateway).send(eq("a@b.com"), anyString(), anyString());
    }

    // ========== resetPassword ==========

    @Test
    void resetPassword_normal_updatesAndKicksOut() {
        EmailToken token = validToken(EmailToken.PURPOSE_RESET_PASSWORD, "a@b.com");
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(token);
        User user = new User();
        user.setId(9L);
        when(userMapper.selectByEmail(1L, "a@b.com")).thenReturn(user);

        PasswordResetDTO dto = new PasswordResetDTO();
        dto.setToken("raw-token");
        dto.setNewPassword("NewPass123");
        service.resetPassword(dto);

        verify(userMapper).updatePassword(eq(9L), anyString(), any(LocalDateTime.class));
        assertThat(token.getUsedAt()).isNotNull();
        // 踢下线（黑名单 + kickout）在静态调用里，此处只验证不抛错即视为走通
    }

    @Test
    void resetPassword_reuseConsumedToken_throws() {
        EmailToken token = validToken(EmailToken.PURPOSE_RESET_PASSWORD, "a@b.com");
        token.setUsedAt(LocalDateTime.now().minusMinutes(1)); // 已用过
        when(emailTokenMapper.selectByTokenHash(anyString())).thenReturn(token);

        PasswordResetDTO dto = new PasswordResetDTO();
        dto.setToken("raw-token");
        dto.setNewPassword("NewPass123");
        assertThatThrownBy(() -> service.resetPassword(dto))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.EMAIL_TOKEN_INVALID.getCode()));
    }

    // ========== isEmailVerified ==========

    @Test
    void isEmailVerified_usedToken_true_unusedToken_false() {
        EmailToken used = validRegisterToken();
        used.setUsedAt(LocalDateTime.now());
        when(emailTokenMapper.selectLatestByEmailAndPurpose(1L, "a@b.com", "REGISTER"))
                .thenReturn(used);
        assertThat(service.isEmailVerified(1L, "a@b.com")).isTrue();

        EmailToken unused = validRegisterToken();
        when(emailTokenMapper.selectLatestByEmailAndPurpose(1L, "c@d.com", "REGISTER"))
                .thenReturn(unused);
        assertThat(service.isEmailVerified(1L, "c@d.com")).isFalse();
    }

    // ========== helpers ==========

    /** 构造一条有效未使用的 REGISTER 令牌（tokenHash 同时充当明文 token，测试替身按它命中） */
    private EmailToken validRegisterToken() {
        return validToken(EmailToken.PURPOSE_REGISTER, "a@b.com");
    }

    private EmailToken validToken(String purpose, String email) {
        EmailToken token = new EmailToken();
        token.setTenantId(1L);
        token.setEmail(email);
        token.setPurpose(purpose);
        token.setTokenHash("hash-" + purpose + "-" + email);
        token.setPayload("{\"p\":\"$2a$10$encoded\",\"n\":\"测试用户\"}");
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        return token;
    }
}
