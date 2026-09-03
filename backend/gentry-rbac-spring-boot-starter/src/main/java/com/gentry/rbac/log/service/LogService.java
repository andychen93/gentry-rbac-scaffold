package com.gentry.rbac.log.service;

import com.gentry.core.common.PageResult;
import com.gentry.rbac.log.dto.*;
import com.gentry.rbac.log.entity.OperLog;
import com.gentry.rbac.log.vo.*;

import java.util.List;

public interface LogService {
    PageResult<OperLogListVO> listOperLogs(OperLogQueryDTO query);
    OperLogDetailVO getOperLogDetail(Long id);
    byte[] exportOperLogs(OperLogQueryDTO query);
    int cleanOperLogs(int beforeDays);
    void saveOperLog(OperLog operLog);
    /** 操作日志「模块」筛选下拉的选项来源（{@code SELECT DISTINCT module}） */
    List<String> listOperLogModules();

    PageResult<LoginLogListVO> listLoginLogs(LoginLogQueryDTO query);
    LoginLogDetailVO getLoginLogDetail(Long id);
    byte[] exportLoginLogs(LoginLogQueryDTO query);
    int cleanLoginLogs(int beforeDays);
    void saveLoginLog(String username, String loginType, String loginIp,
                      String userAgent, int status, String message);
}
