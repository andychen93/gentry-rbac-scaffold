package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 通过邮件令牌重置密码。 */
@Data
public class PasswordResetDTO {

    @NotBlank(message = "{valid.auth.token.notBlank}")
    private String token;

    @NotBlank(message = "{valid.common.password.notBlank}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
             message = "密码8-20位，必须含大小写字母和数字")
    private String newPassword;
}
