package com.precision.rbac.user.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.user.dto.UserQueryDTO;
import com.precision.rbac.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM sys_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted = 0")
    User selectByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted = 0")
    int countByUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    @Select({"<script>",
        "SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND phone = #{phone} AND deleted = 0",
        "<if test='excludeId != null'> AND id != #{excludeId}</if>",
        "</script>"})
    int countByPhone(@Param("tenantId") Long tenantId, @Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Update("UPDATE sys_user SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    int logicDeleteById(@Param("id") Long id);

    @Update("UPDATE sys_user SET password = #{password}, pwd_update_time = #{pwdUpdateTime}, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("pwdUpdateTime") LocalDateTime pwdUpdateTime);

    @Update("UPDATE sys_user SET status = #{status}, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET login_ip = #{loginIp}, login_date = #{loginDate}, update_time = NOW() WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("loginIp") String loginIp, @Param("loginDate") LocalDateTime loginDate);

    /**
     * 分页查询用户列表（XML 实现）
     */
    List<User> selectList(@Param("query") UserQueryDTO query, @Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);

    long selectCount(@Param("query") UserQueryDTO query, @Param("tenantId") Long tenantId, @Param("deptIds") List<Long> deptIds);
}
