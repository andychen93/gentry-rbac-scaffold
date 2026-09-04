package com.gentry.rbac.menu.enums;

/**
 * 菜单类型枚举
 */
public enum MenuType {

    DIR(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮");

    private final int code;
    private final String description;

    MenuType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static MenuType fromCode(int code) {
        for (MenuType mt : values()) {
            if (mt.code == code) return mt;
        }
        throw new IllegalArgumentException("Invalid MenuType code: " + code);
    }
}
