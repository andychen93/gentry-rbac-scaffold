package com.precision.rbac.log.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.log.dto.OperLogQueryDTO;
import com.precision.rbac.log.entity.OperLog;
import com.precision.rbac.log.vo.OperLogListVO;
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
}
