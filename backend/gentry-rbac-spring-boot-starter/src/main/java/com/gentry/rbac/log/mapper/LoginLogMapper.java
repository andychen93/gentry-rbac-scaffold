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

    List<LoginLogListVO> selectList(@Param("query") LoginLogQueryDTO query);

    List<LoginLogListVO> selectExportList(@Param("query") LoginLogQueryDTO query);

    long selectCount(@Param("query") LoginLogQueryDTO query);

    @Delete("DELETE FROM sys_login_log WHERE login_time < #{cutoffTime}")
    int deleteBeforeTime(@Param("cutoffTime") LocalDateTime cutoffTime);
}
