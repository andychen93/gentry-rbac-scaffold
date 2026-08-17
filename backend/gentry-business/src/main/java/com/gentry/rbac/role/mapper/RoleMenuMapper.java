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
