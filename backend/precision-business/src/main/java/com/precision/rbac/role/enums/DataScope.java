package com.precision.rbac.role.enums;

/**
 * 数据权限范围枚举
 */
public enum DataScope {

    ALL(1, "全部数据"),
    DEPT_AND_CHILD(2, "本部门及子部门数据"),
    DEPT_ONLY(3, "本部门数据"),
    SELF_ONLY(4, "仅本人数据"),
    CUSTOM(5, "自定义");

    private final int code;
    private final String description;

    DataScope(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static DataScope fromCode(int code) {
        for (DataScope ds : values()) {
            if (ds.code == code) {
                return ds;
            }
        }
        throw new IllegalArgumentException("Invalid DataScope code: " + code);
    }
}
