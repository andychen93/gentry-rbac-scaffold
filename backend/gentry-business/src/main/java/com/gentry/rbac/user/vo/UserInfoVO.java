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
    /**
     * 是否平台超管。判据与后端一致（角色含 {@code SUPER_ADMIN}），由后端算好下发。
     *
     * <p><b>为什么不让前端自己从 roles 里推</b>：那会把「谁是平台超管」这条规则复制到 TS 里，
     * 判据一变（比如改成角色表上的 role_level 列）就有两处要改，而漏改的那处是**安全判断**。
     * 前端用它在权限分配树里隐藏平台级权限点，见 {@code pages/role/PermissionPage.tsx}。</p>
     */
    private Boolean platformAdmin;
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
