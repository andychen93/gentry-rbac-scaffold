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

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /** 邮箱登录 / 邮箱占用校验：email 无唯一键，靠查询判断，调用方须先做小写规范化 */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0 LIMIT 1")
    User selectByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*) FROM sys_user WHERE email = #{email} AND deleted = 0")
    int countByEmail(@Param("email") String email);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    /** 启用用户的下拉选项（供角色绑定用户的穿梭框） */
    @Select("SELECT u.id, u.username, u.nickname, d.name AS deptName "
            + "FROM sys_user u LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.deleted = 0 "
            + "WHERE u.status = 1 AND u.deleted = 0 "
            + "ORDER BY u.id ASC")
    List<UserOptionVO> selectOptions();

    /**
     * 统计给定 id 中未删除的用户数，用于绑定前校验。
     */
    @Select({"<script>",
            "SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND id IN",
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>",
            "</script>"})
    int countExistingByIds(@Param("userIds") List<Long> userIds);

    @Select({"<script>",
        "SELECT COUNT(*) FROM sys_user WHERE phone = #{phone} AND deleted = 0",
        "<if test='excludeId != null'> AND id != #{excludeId}</if>",
        "</script>"})
    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Update("UPDATE sys_user SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int logicDeleteById(@Param("id") Long id);

    @Update("UPDATE sys_user SET password = #{password}, pwd_update_time = #{pwdUpdateTime}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("pwdUpdateTime") LocalDateTime pwdUpdateTime);

    @Update("UPDATE sys_user SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET language = #{language}, update_time = CURRENT_TIMESTAMP WHERE id = #{id} AND deleted = 0")
    int updateLanguage(@Param("id") Long id, @Param("language") String language);

    @Update("UPDATE sys_user SET login_ip = #{loginIp}, login_date = #{loginDate}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("loginIp") String loginIp, @Param("loginDate") LocalDateTime loginDate);

    /**
     * 分页查询用户列表（XML 实现）
     */
    List<User> selectList(@Param("query") UserQueryDTO query, @Param("deptIds") List<Long> deptIds);

    long selectCount(@Param("query") UserQueryDTO query, @Param("deptIds") List<Long> deptIds);
}
