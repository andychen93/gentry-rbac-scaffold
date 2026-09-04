package com.gentry.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通知接收人查询：解析管理员手机号，用于高级别通知的短信下发。
 */
@Mapper
public interface NotificationRecipientMapper {

    /**
     * 查询启用、且配置了手机号的管理员（ADMIN）手机号。
     *
     * <p>本仓库已拿掉多租户机制（见 {@code doc/design/modules/core/去多租户化-概要设计.md}），
     * SUPER_ADMIN 与 ADMIN 已合并为单一 ADMIN 角色，此前的 tenant_id 限定条件与
     * role_code IN ('ADMIN','SUPER_ADMIN') 一并简化。</p>
     */
    @Select("SELECT DISTINCT u.phone FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id "
            + "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 "
            + "WHERE u.deleted = 0 AND u.status = 1 "
            + "AND u.phone IS NOT NULL AND u.phone <> '' "
            + "AND r.role_code = 'ADMIN'")
    List<String> selectAdminPhones();
}
