package com.gentry.rbac.role.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.role.entity.RoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色菜单关联 Mapper
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    /**
     * 批量插入角色菜单关联
     */
    int batchInsertRoleMenu(@Param("list") List<RoleMenuEntry> list);

    /**
     * 批量插入角色菜单关联（使用 RoleMenu 实体）
     */
    int batchInsert(@Param("list") List<RoleMenu> list);

    /**
     * 查询所有菜单ID
     */
    List<Long> selectAllMenuIds();

    /**
     * 查询所有**租户级**菜单 ID（{@code is_platform = 0}）。
     *
     * <p>新建租户时给它的 ADMIN 角色用 —— 「租户下的最高权限」= 除平台级之外的全部。
     * 原来用 {@link #selectAllMenuIds()}，于是任何租户的管理员都拿到了
     * {@code system:tenant:*}，能管理所有租户。</p>
     */
    @Select("SELECT id FROM sys_menu WHERE deleted = 0 AND is_platform = 0")
    List<Long> selectTenantScopedMenuIds();

    /**
     * 从给定 ID 集合里挑出平台级的那些，用于校验「调用者有没有权分配这批菜单」。
     *
     * <p>返回非空即表示越权，调用方据此抛 {@code PLATFORM_MENU_FORBIDDEN}。
     * 之所以返回具体 ID 而不是布尔值：服务端日志要留下审计痕迹（试图分配哪些权限点）。
     * 给用户的报错文案里不带这些 ID —— 不该告知攻击者「哪几个是平台级」。</p>
     */
    @Select({"<script>",
            "SELECT id FROM sys_menu WHERE deleted = 0 AND is_platform = 1 AND id IN",
            "<foreach collection='menuIds' item='menuId' open='(' separator=',' close=')'>",
            "#{menuId}",
            "</foreach>",
            "</script>"})
    List<Long> selectPlatformMenuIdsIn(@Param("menuIds") List<Long> menuIds);

    /**
     * 根据角色ID查询菜单ID列表
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据菜单ID列表批量删除角色菜单关联
     */
    int deleteByMenuIds(@Param("menuIds") List<Long> menuIds);

    /**
     * 统计角色关联的用户数
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID列表查询权限标识列表
     */
    List<String> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据角色ID列表查询去重的菜单ID列表
     */
    @Select({"<script>",
            "SELECT DISTINCT menu_id FROM sys_role_menu WHERE role_id IN",
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>",
            "#{roleId}",
            "</foreach>",
            "</script>"})
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 角色菜单关联条目（兼容旧代码）
     */
    class RoleMenuEntry {
        private Long id;
        private Long roleId;
        private Long menuId;

        public RoleMenuEntry() {}
        public RoleMenuEntry(Long id, Long roleId, Long menuId) {
            this.id = id;
            this.roleId = roleId;
            this.menuId = menuId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public Long getMenuId() { return menuId; }
        public void setMenuId(Long menuId) { this.menuId = menuId; }
    }
}
