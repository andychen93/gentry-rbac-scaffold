package com.gentry.rbac.log.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.log.dto.LoginLogQueryDTO;
import com.gentry.rbac.log.entity.LoginLog;
import com.gentry.rbac.log.vo.LoginLogListVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    List<LoginLogListVO> selectList(@Param("query") LoginLogQueryDTO query, @Param("tenantId") Long tenantId);

    List<LoginLogListVO> selectExportList(@Param("query") LoginLogQueryDTO query, @Param("tenantId") Long tenantId);

    long selectCount(@Param("query") LoginLogQueryDTO query, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM sys_login_log WHERE tenant_id = #{tenantId} AND login_time < #{cutoffTime}")
    int deleteBeforeTime(@Param("tenantId") Long tenantId, @Param("cutoffTime") LocalDateTime cutoffTime);

    /** 跨租户清理过期日志（定时任务用，需配合 TenantManager.ignoreTenantCondition） */
    @Delete("DELETE FROM sys_login_log WHERE login_time < #{cutoffTime}")
    int cleanExpiredBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}
