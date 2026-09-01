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
    /**
     * 语言偏好，三态：{@code null} = 从未选过（前端保持当前 locale，即跟随浏览器）；
     * {@code zh_CN} / {@code en_US} = 显式选过，前端应切到该语言。
     *
     * <p>为什么要下发：登录页用的可能是浏览器默认语言，而用户偏好是另一种。
     * 不带这个字段，前端只能先按浏览器语言渲染再纠正，界面会闪。</p>
     */
    private String language;
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
