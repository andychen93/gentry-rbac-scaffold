package com.precision.rbac.user.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户导出 Excel 行 VO（easyexcel 注解驱动）。
 */
@Data
public class UserExportVO {
    @ExcelProperty("用户名") private String username;
    @ExcelProperty("昵称") private String nickname;
    @ExcelProperty("手机号") private String phone;
    @ExcelProperty("邮箱") private String email;
    @ExcelProperty("性别") private String gender;
    @ExcelProperty("部门") private String deptName;
    @ExcelProperty("职务") private String postName;
    @ExcelProperty("状态") private String status;
    @ExcelProperty("创建时间") private String createTime;
}
