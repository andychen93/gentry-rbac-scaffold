package com.gentry.rbac.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.List;

/**
 * 用户导入 Excel 行 DTO。
 *
 * <p><b>按列序匹配，不按表头文本匹配。</b>原来用 {@code @ExcelProperty("用户名")}
 * 是拿表头文本对列的，一旦导出表头被本地化，英文用户导出的 Excel 再导回来表头是
 * {@code Username} 而 DTO 期待「用户名」，<b>列全部匹配不上、导入静默得到全 null</b>。
 * 改成 {@code index} 之后，中英文任意语言导出的文件都能导回来。</p>
 *
 * <p>用户名必填，其余可空；初始密码由系统统一分配（{@code DEFAULT_IMPORT_PASSWORD}）。</p>
 */
@Data
public class UserImportDTO {

    @ExcelProperty(index = 0) private String username;
    @ExcelProperty(index = 1) private String nickname;
    @ExcelProperty(index = 2) private String phone;
    @ExcelProperty(index = 3) private String email;
    /**
     * 性别，<b>用 String 收而不是 Integer</b>。
     *
     * <p>这里修的是一个既有缺陷：导出写的是「男」/{@code Male}（导出是给人看的报表），
     * 而本字段原来是 {@code Integer}，于是<b>导出的文件导回来这一列静默变 null</b>，
     * 与上面那句「任意语言导出的文件都能导回来」直接矛盾。</p>
     *
     * <p>改成 String 之后由 {@code LocalizedCodeResolver} 归一：码（0/1/2）与
     * 任一语言的 label（男/女/未知、Male/Female/Unknown）都认。</p>
     */
    @ExcelProperty(index = 4) private String gender;
    /**
     * 职务，收的是<b>字典码或任一语言的 label</b>，由 {@code LocalizedCodeResolver} 归一成码。
     *
     * <p>{@code sys_user.post_name} 自 V13 起存 {@code sys_user_post} 的码
     * （{@code Manager}）而不是中文 label（「经理」）—— 存展示文案会让职务下拉无法翻译。</p>
     */
    @ExcelProperty(index = 5) private String postName;

    /** 模板表头 i18n key，顺序与上面的 index 对应 */
    public static final List<String> HEAD_KEYS = List.of(
            "export.user.username",
            "export.user.nickname",
            "export.user.phone",
            "export.user.email",
            "export.user.gender.hint",
            "export.user.post");
}
