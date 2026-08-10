package com.precision.rbac.user.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户导入结果统计。
 */
@Data
public class UserImportResultVO {

    /** 成功条数 */
    private int success;
    /** 失败条数 */
    private int fail;
    /** 失败明细 */
    private List<ErrorItem> errors;

    public UserImportResultVO() {}

    public UserImportResultVO(int success, int fail, List<ErrorItem> errors) {
        this.success = success;
        this.fail = fail;
        this.errors = errors;
    }

    @Data
    public static class ErrorItem {
        /** Excel 行号（含表头，从 1 开始；数据行 +2） */
        private int row;
        private String username;
        private String msg;

        public ErrorItem() {}

        public ErrorItem(int row, String username, String msg) {
            this.row = row;
            this.username = username;
            this.msg = msg;
        }
    }
}
