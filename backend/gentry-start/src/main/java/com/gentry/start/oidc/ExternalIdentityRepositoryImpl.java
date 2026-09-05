package com.gentry.start.oidc;

import com.gentry.oidc.model.NormalizedIdentity;
import com.gentry.oidc.service.ExternalIdentityRepository;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.entity.UserRole;
import com.gentry.rbac.user.mapper.UserMapper;
import com.gentry.rbac.user.mapper.UserRoleMapper;
import com.gentry.start.oidc.entity.ExternalIdentityEntity;
import com.gentry.start.oidc.mapper.ExternalIdentityMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * {@link ExternalIdentityRepository} 的落库实现。
 * <p>作为 gentry-start 的狗粮业务实现：查询/创建 sys_external_identity 映射，
 * 首次 SSO 登录时 JIT 创建 sys_user 并绑定默认角色。</p>
 *
 * <p><b>密码字段说明</b>：SSO 用户不通过本地密码登录，写入一个不可逆的随机
 * 占位密码（非明文可猜测值），防止误留后门；用户名唯一索引沿用
 * sys_user.uk_user_username。</p>
 */
@Slf4j
@Component
public class ExternalIdentityRepositoryImpl implements ExternalIdentityRepository {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExternalIdentityMapper externalIdentityMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleCodeLookupMapper roleCodeLookupMapper;

    public ExternalIdentityRepositoryImpl(ExternalIdentityMapper externalIdentityMapper,
                                            UserMapper userMapper,
                                            UserRoleMapper userRoleMapper,
                                            RoleCodeLookupMapper roleCodeLookupMapper) {
        this.externalIdentityMapper = externalIdentityMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleCodeLookupMapper = roleCodeLookupMapper;
    }

    @Override
    public Long findLocalUserId(String identityProvider, String externalSubject) {
        return externalIdentityMapper.selectLocalUserId(identityProvider, externalSubject);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUserWithIdentity(NormalizedIdentity identity, String defaultRole) {
        User user = new User();
        String claimedUsername = resolveUsername(identity);
        user.setUsername(uniqueUsername(claimedUsername, identity));
        user.setPassword(randomUnusablePassword());
        user.setNickname(resolveNickname(identity));
        user.setEmail(identity.getEmail());
        user.setGender(0);
        user.setStatus(1);
        user.setLanguage(identity.getLanguage());
        userMapper.insert(user);

        ExternalIdentityEntity mapping = new ExternalIdentityEntity();
        mapping.setIdentityProvider(identity.getIdentityProvider());
        mapping.setExternalSubject(identity.getExternalSubject());
        mapping.setLocalUserId(user.getId());
        // 审计：记录 IdP 侧原始用户名（本地用户名被冲突改写时据此溯源）
        mapping.setExtraAttributes("{\"claimed_username\":\"" + claimedUsername + "\"}");
        externalIdentityMapper.insert(mapping);

        Long roleId = roleCodeLookupMapper.selectIdByRoleCode(defaultRole);
        if (roleId != null) {
            userRoleMapper.insert(new UserRole(user.getId(), roleId));
        } else {
            log.warn("Default role code [{}] not found, skip role binding for JIT user {}",
                defaultRole, user.getId());
        }

        return user.getId();
    }

    /**
     * 同名冲突策略：本地已存在同名用户时派生新用户名，<b>绝不自动绑定</b>。
     * <p>安全考量：本地同名用户（尤其 admin 这类超管）与 IdP 侧同名账号
     * 并非同一自然人——自动绑定等于允许任何能在 IdP 注册该用户名的人
     * 继承本地账号的全部权限（提权漏洞）。正确姿势是创建独立的本地账号
     * （{@code {name}.{provider}}，再撞则追加 subject 短后缀），映射关系
     * 只认 external_subject，与用户名无关；如确需合并账号，由管理员在
     * 后台手动改绑。</p>
     */
    private String uniqueUsername(String claimed, NormalizedIdentity identity) {
        if (userMapper.selectByUsername(claimed) == null) {
            return claimed;
        }
        String candidate = claimed + "." + identity.getIdentityProvider();
        if (userMapper.selectByUsername(candidate) == null) {
            log.warn("Username [{}] taken by local user; JIT created [{}] instead (no auto-binding)", claimed, candidate);
            return candidate;
        }
        String subSuffix = identity.getExternalSubject().replaceAll("[^a-zA-Z0-9]", "");
        if (subSuffix.length() > 8) {
            subSuffix = subSuffix.substring(0, 8);
        }
        candidate = candidate + "." + subSuffix;
        if (userMapper.selectByUsername(candidate) != null) {
            // 理论上到不了这里（subject 全局唯一）；防御性兜底
            throw new IllegalStateException("Unable to derive unique username for externalSubject="
                + identity.getExternalSubject());
        }
        log.warn("Username [{}] and [{}.{}] both taken; JIT created [{}]", claimed, claimed,
            identity.getIdentityProvider(), candidate);
        return candidate;
    }

    private String resolveUsername(NormalizedIdentity identity) {
        if (identity.getUsername() != null && !identity.getUsername().isBlank()) {
            return identity.getUsername();
        }
        // 兜底：用 externalSubject 派生一个唯一用户名，避免用户名冲突
        return "kc_" + identity.getExternalSubject().replaceAll("[^a-zA-Z0-9]", "").substring(0,
            Math.min(20, identity.getExternalSubject().replaceAll("[^a-zA-Z0-9]", "").length()));
    }

    private String resolveNickname(NormalizedIdentity identity) {
        if (identity.getDisplayName() != null && !identity.getDisplayName().isBlank()) {
            return identity.getDisplayName();
        }
        return resolveUsername(identity);
    }

    /**
     * 生成不可用于本地登录的随机密码占位符（SSO 用户不走密码登录路径）。
     */
    private String randomUnusablePassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "$sso$" + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 按角色编码查 ID 的最小查询接口。
     * <p>不复用/修改 rbac-starter 的 RoleMapper（避免改动共享代码），
     * 独立声明一个只读查询接口。</p>
     */
    @Mapper
    public interface RoleCodeLookupMapper {
        @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} AND deleted = 0 AND status = 1 LIMIT 1")
        Long selectIdByRoleCode(@Param("roleCode") String roleCode);
    }
}
