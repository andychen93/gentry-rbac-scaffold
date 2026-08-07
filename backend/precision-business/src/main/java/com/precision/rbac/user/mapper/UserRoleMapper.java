package com.precision.rbac.user.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.user.entity.UserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    int batchInsert(@Param("list") List<UserRole> list);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 1")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT ur.user_id FROM sys_user_role ur WHERE ur.role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
