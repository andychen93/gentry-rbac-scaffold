package com.gentry.rbac.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户导入 Excel 行 DTO（easyexcel 注解驱动，表头即 @ExcelProperty value）。
 * 用户名必填，其余可空；初始密码由系统统一分配（DEFAULT_IMPORT_PASSWORD）。
 */
@Data
public class UserImportDTO {
    @ExcelProperty("用户名") private String username;
    @ExcelProperty("昵称") private String nickname;
    @ExcelProperty("手机号") private String phone;
    @ExcelProperty("邮箱") private String email;
    @ExcelProperty("性别(0未知1男2女)") private Integer gender;
    @ExcelProperty("职务") private String postName;
}
