package com.precision.rbac.user.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.user.entity.UserRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /** 解除某角色的全部用户关联（角色绑定用户时先删后插） */
    @Delete("DELETE FROM sys_user_role WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    int batchInsert(@Param("list") List<UserRole> list);

    /**
     * 查用户已分配的角色 id。
     *
     * <p>必须 JOIN sys_role 过滤 deleted：直接读 sys_user_role 会把「角色行已不存在
     * 或已逻辑删除」的脏关联也返回，前端「分配角色」弹窗以它初始化选中项，
     * 而候选列表（roleApi.options）不含这些角色 → 该 id 在界面上不可见却会被原样提交，
     * 后端 validateRoleIds 再报「角色不存在: [xxx]」，用户完全无从下手。</p>
     */
    @Select("SELECT ur.role_id FROM sys_user_role ur "
            + "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 "
            + "WHERE ur.user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 1")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT ur.user_id FROM sys_user_role ur WHERE ur.role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
