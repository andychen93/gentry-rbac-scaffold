package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 邮箱验证 / 密码重置的令牌载体。 */
@Data
public class TokenDTO {

    @NotBlank(message = "{valid.auth.token.notBlank}")
    private String token;
}
