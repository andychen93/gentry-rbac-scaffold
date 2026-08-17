package com.gentry.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通知接收人查询：解析租户内管理员手机号，用于高级别通知的短信下发。
 */
@Mapper
public interface NotificationRecipientMapper {

    /**
     * 查询某租户内启用、且配置了手机号的管理员（ADMIN/SUPER_ADMIN）手机号。
     *
     * <p>用户、用户角色、角色三个租户级表均显式限定同一 tenant_id；调用方需在
     * 租户拦截器忽略上下文中执行，避免异步线程无租户上下文时被错误追加默认租户。</p>
     */
    @Select("SELECT DISTINCT u.phone FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id AND ur.tenant_id = u.tenant_id "
            + "JOIN sys_role r ON r.id = ur.role_id AND r.tenant_id = u.tenant_id AND r.deleted = 0 "
            + "WHERE u.tenant_id = #{tenantId} AND u.deleted = 0 AND u.status = 1 "
            + "AND u.phone IS NOT NULL AND u.phone <> '' "
            + "AND r.role_code IN ('ADMIN','SUPER_ADMIN')")
    List<String> selectAdminPhones(@Param("tenantId") Long tenantId);
}
