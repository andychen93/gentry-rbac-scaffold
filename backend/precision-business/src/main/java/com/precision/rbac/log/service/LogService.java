package com.precision.rbac.log.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.log.dto.*;
import com.precision.rbac.log.entity.OperLog;
import com.precision.rbac.log.vo.*;

import java.util.List;

public interface LogService {
    PageResult<OperLogListVO> listOperLogs(OperLogQueryDTO query);
    OperLogDetailVO getOperLogDetail(Long id);
    byte[] exportOperLogs(OperLogQueryDTO query);
    int cleanOperLogs(int beforeDays);
    void saveOperLog(OperLog operLog);

    PageResult<LoginLogListVO> listLoginLogs(LoginLogQueryDTO query);
    LoginLogDetailVO getLoginLogDetail(Long id);
    byte[] exportLoginLogs(LoginLogQueryDTO query);
    int cleanLoginLogs(int beforeDays);
    void saveLoginLog(String username, Long tenantId, String loginType, String loginIp,
                      String userAgent, int status, String message);
}
