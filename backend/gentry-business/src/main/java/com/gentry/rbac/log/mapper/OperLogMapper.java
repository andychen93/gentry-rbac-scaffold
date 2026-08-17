package com.gentry.rbac.log.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.log.dto.OperLogQueryDTO;
import com.gentry.rbac.log.entity.OperLog;
import com.gentry.rbac.log.vo.OperLogListVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperLogMapper extends BaseMapper<OperLog> {

    List<OperLogListVO> selectList(@Param("query") OperLogQueryDTO query, @Param("tenantId") Long tenantId);

    List<OperLogListVO> selectExportList(@Param("query") OperLogQueryDTO query, @Param("tenantId") Long tenantId);

    long selectCount(@Param("query") OperLogQueryDTO query, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM sys_oper_log WHERE tenant_id = #{tenantId} AND operate_time < #{cutoffTime}")
    int deleteBeforeTime(@Param("tenantId") Long tenantId, @Param("cutoffTime") LocalDateTime cutoffTime);

    /** 跨租户清理过期日志（定时任务用，需配合 TenantManager.ignoreTenantCondition） */
    @Delete("DELETE FROM sys_oper_log WHERE operate_time < #{cutoffTime}")
    int cleanExpiredBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}
