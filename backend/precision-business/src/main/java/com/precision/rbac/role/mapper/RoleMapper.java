package com.precision.rbac.role.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.role.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 分页列表查询（含 userCount 统计）
     */
    List<Map<String, Object>> selectPageList(@Param("tenantId") Long tenantId,
                                              @Param("roleName") String roleName,
                                              @Param("roleCode") String roleCode,
                                              @Param("status") Integer status,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 统计总数
     */
    long selectCount(@Param("tenantId") Long tenantId,
                     @Param("roleName") String roleName,
                     @Param("roleCode") String roleCode,
                     @Param("status") Integer status);

    /**
     * 检查角色编码是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_role WHERE tenant_id = #{tenantId} AND role_code = #{roleCode} AND deleted = 0")
    int countByCode(@Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);

    /**
     * 逻辑删除角色
     */
    @Update("UPDATE sys_role SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(@Param("id") Long id);

    /**
     * 更新数据权限范围
     */
    @Update("UPDATE sys_role SET data_scope = #{dataScope}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updateDataScope(@Param("id") Long id, @Param("dataScope") Integer dataScope);

    /**
     * 更新角色状态
     */
    @Update("UPDATE sys_role SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据角色ID列表查询角色信息
     */
    @Select({"<script>",
        "SELECT * FROM sys_role WHERE id IN",
        "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "AND deleted = 0",
        "</script>"})
    List<Role> selectByIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据ID查询角色（使用XML ResultMap映射）
     */
    Role selectRoleById(@Param("id") Long id);

}
