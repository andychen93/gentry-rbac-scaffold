package com.gentry.rbac.user.vo;

import lombok.Data;

import com.gentry.rbac.menu.vo.MenuTreeVO;

import java.util.List;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long deptId;
    private String deptName;
    private List<RoleInfo> roles;
    private List<String> permissions;
    private List<MenuTreeVO> menus;

    @Data
    public static class RoleInfo {
        private Long id;
        private String roleCode;
        private String roleName;
        private Integer dataScope;

        public RoleInfo() {}
        public RoleInfo(Long id, String roleCode, String roleName, Integer dataScope) {
            this.id = id;
            this.roleCode = roleCode;
            this.roleName = roleName;
            this.dataScope = dataScope;
        }
    }
}
