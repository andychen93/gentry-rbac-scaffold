package com.gentry.rbac.user.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.List;

/**
 * 用户导出 Excel 行 VO。
 *
 * <p><b>注解只承载列序，不承载文案。</b>{@code @ExcelProperty("用户名")} 的值是注解常量，
 * 运行时无法本地化；表头改为在 Service 里按当前 locale 现场构建（见
 * {@code UserServiceImpl.exportUsers}），本类只用 {@code index} 固定列顺序。</p>
 */
@Data
public class UserExportVO {

    @ExcelProperty(index = 0) private String username;
    @ExcelProperty(index = 1) private String nickname;
    @ExcelProperty(index = 2) private String phone;
    @ExcelProperty(index = 3) private String email;
    @ExcelProperty(index = 4) private String gender;
    @ExcelProperty(index = 5) private String deptName;
    @ExcelProperty(index = 6) private String postName;
    @ExcelProperty(index = 7) private String status;
    @ExcelProperty(index = 8) private String createTime;

    /**
     * 表头 i18n key，<b>顺序与上面的 index 严格对应</b>。
     *
     * <p>加字段必须同时加 key，否则表头与数据列错位——由
     * {@code ExportHeadConsistencyTest} 断言长度一致兜住。</p>
     */
    public static final List<String> HEAD_KEYS = List.of(
            "export.user.username",
            "export.user.nickname",
            "export.user.phone",
            "export.user.email",
            "export.user.gender",
            "export.user.dept",
            "export.user.post",
            "export.user.status",
            "export.user.createTime");
}
