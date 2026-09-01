package com.gentry.rbac.dept.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.dept.dto.DeptQueryDTO;
import com.gentry.rbac.dept.entity.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 查询部门列表（按 name/status 过滤，按 sort 排序）
     */
    List<Dept> selectList(@Param("query") DeptQueryDTO query);

    /**
     * 逻辑删除部门
     */
    @Update("UPDATE sys_dept SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int logicDeleteById(@Param("id") Long id);

    /**
     * 统计子部门数量
     */
    @Select("SELECT COUNT(*) FROM sys_dept WHERE parent_id = #{parentId} AND deleted = 0")
    int countByParentId(@Param("parentId") Long parentId);

    /**
     * 查询子孙部门（ancestors LIKE 前缀匹配）
     */
    @Select("SELECT * FROM sys_dept WHERE ancestors LIKE CONCAT(#{ancestorsPrefix}, '%') AND deleted = 0")
    List<Dept> selectByAncestorsLike(@Param("ancestorsPrefix") String ancestorsPrefix);

    /**
     * 统计部门下用户数
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND deleted = 0")
    int countUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 批量统计各部门用户数
     */
    List<Map<String, Object>> countUsersGroupByDept();

    /**
     * 检查部门名称是否存在
     */
    @Select({"<script>",
        "SELECT COUNT(*) FROM sys_dept WHERE name = #{name} AND deleted = 0",
        "<if test='excludeId != null'> AND id != #{excludeId}</if>",
        "</script>"})
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 更新子孙部门的 ancestors
     */
    @Update("UPDATE sys_dept SET ancestors = REPLACE(ancestors, #{oldAncestors}, #{newAncestors}), update_time = CURRENT_TIMESTAMP WHERE ancestors LIKE CONCAT(#{oldAncestors}, '%') AND deleted = 0")
    int updateChildrenAncestors(@Param("oldAncestors") String oldAncestors, @Param("newAncestors") String newAncestors);
}
