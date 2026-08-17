package com.gentry.rbac.user.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.user.dto.UserQueryDTO;
import com.gentry.rbac.user.entity.User;
import com.gentry.rbac.user.vo.UserOptionVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted = 0")
    User selectByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted = 0")
    int countByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    /** 租户内启用用户的下拉选项（供角色绑定用户的穿梭框） */
    @Select("SELECT u.id, u.username, u.nickname, d.name AS deptName "
            + "FROM sys_user u LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.deleted = 0 "
            + "WHERE u.tenant_id = #{tenantId} AND u.status = 1 AND u.deleted = 0 "
            + "ORDER BY u.id ASC")
    List<UserOptionVO> selectOptions(@Param("tenantId") Long tenantId);

    /**
     * 统计给定 id 中属于本租户且未删除的用户数，用于绑定前校验。
     * 带 tenant_id 是防越权：不能把别的租户的用户绑到本租户角色上。
     */
    @Select({"<script>",
            "SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND deleted = 0 AND id IN",
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>",
            "</script>"})
    int countExistingByIds(@Param("tenantId") Long tenantId, @Param("userIds") List<Long> userIds);

    @Select({"<script>",
        "SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND phone = #{phone} AND deleted = 0",
        "<if test='excludeId != null'> AND id != #{excludeId}</if>",
        "</script>"})
    int countByPhone(@Param("tenantId") Long tenantId, @Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Update("UPDATE sys_user SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int logicDeleteById(@Param("id") Long id);

    @Update("UPDATE sys_user SET password = #{password}, pwd_update_time = #{pwdUpdateTime}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("pwdUpdateTime") LocalDateTime pwdUpdateTime);

    @Update("UPDATE sys_user SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET login_ip = #{loginIp}, login_date = #{loginDate}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("loginIp") String loginIp, @Param("loginDate") LocalDateTime loginDate);

    /**
     * 分页查询用户列表（XML 实现）
     */
    List<User> selectList(@Param("query") UserQueryDTO query, @Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    long selectCount(@Param("query") UserQueryDTO query, @Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);
}
