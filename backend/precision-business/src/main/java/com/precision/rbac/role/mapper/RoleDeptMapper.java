package com.precision.rbac.role.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.role.entity.RoleDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色部门关联 Mapper
 */
@Mapper
public interface RoleDeptMapper extends BaseMapper<RoleDept> {

    /**
     * 根据角色ID删除关联
     */
    @Delete("DELETE FROM sys_role_dept WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色部门关联
     */
    int batchInsert(@Param("list") List<RoleDept> list);

    /**
     * 根据角色ID查询部门ID列表
     */
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);
}
