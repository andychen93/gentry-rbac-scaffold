package com.gentry.rbac.menu.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.menu.dto.MenuQueryDTO;
import com.gentry.rbac.menu.entity.Menu;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 查询菜单列表（按 name/status/type 过滤，按 sort 排序）
     */
    List<Menu> selectList(@Param("query") MenuQueryDTO query);

    /**
     * 批量逻辑删除菜单
     */
    int batchLogicDelete(@Param("ids") List<Long> ids);

    /**
     * 查询所有菜单 ID（未删除）
     */
    @Select("SELECT id FROM sys_menu WHERE deleted = 0")
    List<Long> selectAllIds();

    /**
     * 查询指定父级下的子菜单
     */
    @Select("SELECT * FROM sys_menu WHERE parent_id = #{parentId} AND deleted = 0")
    List<Menu> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 检查同级菜单名称是否存在
     */
    @Select({"<script>",
        "SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId} AND name = #{name} AND deleted = 0",
        "<if test='excludeId != null'> AND id != #{excludeId}</if>",
        "</script>"})
    int countByName(@Param("parentId") Long parentId, @Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 根据ID列表批量查询菜单（type IN 1,2，status=1，visible=1，未删除）
     */
    @Select({"<script>",
        "SELECT * FROM sys_menu WHERE deleted = 0 AND status = 1 AND visible = 1 AND type IN (1, 2) AND id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "ORDER BY sort ASC, create_time ASC",
        "</script>"})
    List<Menu> selectNavByIds(@Param("ids") List<Long> ids);

    /**
     * 根据ID列表批量查询菜单（不限 type/status/visible，用于向上查找父节点）
     */
    @Select({"<script>",
        "SELECT * FROM sys_menu WHERE deleted = 0 AND id IN",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "</script>"})
    List<Menu> selectByIds(@Param("ids") List<Long> ids);
}
