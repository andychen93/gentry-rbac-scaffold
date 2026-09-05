package com.gentry.oidc.event;

import com.gentry.oidc.model.NormalizedIdentity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * OIDC 认证审计事件。
 * <p>登录成功/失败、JIT 创建用户、登出均发布此事件，由消费方（如
 * rbac-starter 的登录日志模块）订阅并落库，避免 oidc-starter 直接依赖
 * rbac-starter 的内部表结构。</p>
 */
@Getter
public class OidcAuthEvent extends ApplicationEvent {

    /** 事件类型。 */
    public enum Type {
        /** 登录成功（已存在用户）。 */
        LOGIN_SUCCESS,
        /** 登录成功且触发 JIT 创建用户。 */
        JIT_USER_CREATED,
        /** 登录失败（Token 验证失败或用户被禁用）。 */
        LOGIN_FAILURE,
        /** 登出（Token 主动失效）。 */
        LOGOUT
    }

    private final Type type;
    private final NormalizedIdentity identity;
    private final String remark;

    public OidcAuthEvent(Type type, NormalizedIdentity identity, String remark) {
        super(identity);
        this.type = type;
        this.identity = identity;
        this.remark = remark;
    }
}
