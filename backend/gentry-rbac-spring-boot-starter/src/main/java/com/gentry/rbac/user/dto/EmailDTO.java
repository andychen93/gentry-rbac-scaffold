package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 重发验证邮件 / 忘记密码的邮箱载体。 */
@Data
public class EmailDTO {

    @NotBlank(message = "{valid.auth.email.notBlank}")
    @Email(message = "{valid.common.email.email}")
    @Size(max = 255, message = "{valid.auth.email.size}")
    private String email;
}
