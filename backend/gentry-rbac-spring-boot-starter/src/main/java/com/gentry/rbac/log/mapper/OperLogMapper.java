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

    List<OperLogListVO> selectList(@Param("query") OperLogQueryDTO query);

    List<OperLogListVO> selectExportList(@Param("query") OperLogQueryDTO query);

    long selectCount(@Param("query") OperLogQueryDTO query);

    @Delete("DELETE FROM sys_oper_log WHERE operate_time < #{cutoffTime}")
    int deleteBeforeTime(@Param("cutoffTime") LocalDateTime cutoffTime);
}
