package com.gentry.rbac.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserPasswordUpdateDTO {

    @NotBlank(message = "{valid.user.oldPassword.notBlank}")
    private String oldPassword;

    @NotBlank(message = "{valid.user.newPassword.notBlank}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
             message = "密码8-20位，必须含大小写字母和数字")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
