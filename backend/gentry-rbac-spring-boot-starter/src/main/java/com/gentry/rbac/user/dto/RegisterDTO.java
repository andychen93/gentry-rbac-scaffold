package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱自助注册请求。
 *
 * <p>密码强度与 {@code UserCreateDTO} 保持同一正则：大小写字母 + 数字，8-20 位。
 * 验证通过前不建用户，密码哈希暂存于令牌 payload。</p>
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "{valid.auth.email.notBlank}")
    @Email(message = "{valid.common.email.email}")
    @Size(max = 255, message = "{valid.auth.email.size}")
    private String email;

    @NotBlank(message = "{valid.common.password.notBlank}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
             message = "密码8-20位，必须含大小写字母和数字")
    private String password;

    @NotBlank(message = "{valid.user.nickname.notBlank}")
    @Size(min = 2, max = 20, message = "{valid.user.nickname.size}")
    private String nickname;
}
