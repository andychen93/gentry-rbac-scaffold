package com.gentry.rbac.log.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.i18n.I18nUtil;
import com.gentry.core.security.UserContext;
import com.gentry.core.util.IpUtil;
import com.gentry.rbac.log.dto.*;
import com.gentry.rbac.log.entity.LoginLog;
import com.gentry.rbac.log.entity.OperLog;
import com.gentry.rbac.log.mapper.LoginLogMapper;
import com.gentry.rbac.log.mapper.OperLogMapper;
import com.gentry.rbac.log.service.LogService;
import com.gentry.rbac.log.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    private static final Logger log = LoggerFactory.getLogger(LogServiceImpl.class);
    private final OperLogMapper operLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final I18nUtil i18nUtil;

    public LogServiceImpl(OperLogMapper operLogMapper, LoginLogMapper loginLogMapper, I18nUtil i18nUtil) {
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.i18nUtil = i18nUtil;
    }

    @Override
    public PageResult<OperLogListVO> listOperLogs(OperLogQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        long total = operLogMapper.selectCount(query, tenantId);
        List<OperLogListVO> list = total > 0 ? operLogMapper.selectList(query, tenantId) : List.of();
        // 应用层国际化翻译
        for (OperLogListVO vo : list) {
            vo.setTypeLabel(i18nUtil.getOperLogTypeLabel(vo.getType()));
            vo.setModuleLabel(vo.getModule());
        }
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public OperLogDetailVO getOperLogDetail(Long id) {
        // selectOneById() 已通过全局 tenantColumn 配置自动追加租户条件
        OperLog entity = operLogMapper.selectOneById(id);
        if (entity == null) throw new BizException(ErrorCode.PARAM_ERROR, "日志不存在");
        OperLogDetailVO vo = new OperLogDetailVO();
        vo.setId(entity.getId()); vo.setModule(entity.getModule()); vo.setModuleLabel(entity.getModule());
        vo.setType(entity.getType()); vo.setTypeLabel(i18nUtil.getOperLogTypeLabel(entity.getType()));
        vo.setModuleLabel(entity.getModule());
        vo.setTitle(entity.getTitle()); vo.setOperator(entity.getOperator()); vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorIp(entity.getOperatorIp()); vo.setLocation(entity.getLocation()); vo.setMethod(entity.getMethod());
        vo.setRequestUrl(entity.getRequestUrl()); vo.setRequestParams(entity.getRequestParams());
        vo.setResponseResult(entity.getResponseResult()); vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg()); vo.setCostTime(entity.getCostTime()); vo.setOperateTime(entity.getOperateTime());
        return vo;
    }

    @Override
    public byte[] exportOperLogs(OperLogQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        List<OperLogListVO> list = operLogMapper.selectExportList(query, tenantId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("日志编号,操作模块,动作,标题,操作用户,IP地址,地点,状态,耗时(ms),操作时间\n");
        for (OperLogListVO vo : list) {
            csv.append(csv(vo.getId())).append(',')
                    .append(csv(vo.getModule())).append(',')
                    .append(csv(vo.getType())).append(',')
                    .append(csv(vo.getTitle())).append(',')
                    .append(csv(vo.getOperator())).append(',')
                    .append(csv(vo.getOperatorIp())).append(',')
                    .append(csv(vo.getLocation())).append(',')
                    .append(csv(vo.getStatus() != null && vo.getStatus() == 1 ? "成功" : "失败")).append(',')
                    .append(csv(vo.getCostTime())).append(',')
                    .append(csv(vo.getOperateTime()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int cleanOperLogs(int beforeDays) {
        Long tenantId = UserContext.getTenantId();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(beforeDays);
        return operLogMapper.deleteBeforeTime(tenantId, cutoff);
    }

    @Override
    public void saveOperLog(OperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.error("Failed to save oper log", e);
        }
    }

    @Override
    public PageResult<LoginLogListVO> listLoginLogs(LoginLogQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        long total = loginLogMapper.selectCount(query, tenantId);
        List<LoginLogListVO> list = total > 0 ? loginLogMapper.selectList(query, tenantId) : List.of();
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public LoginLogDetailVO getLoginLogDetail(Long id) {
        // selectOneById() 已通过 @Column(tenantId=true) 自动追加租户条件
        LoginLog entity = loginLogMapper.selectOneById(id);
        if (entity == null) throw new BizException(ErrorCode.PARAM_ERROR, "日志不存在");
        LoginLogDetailVO vo = new LoginLogDetailVO();
        vo.setId(entity.getId()); vo.setUsername(entity.getUsername()); vo.setLoginType(entity.getLoginType());
        vo.setLoginIp(entity.getLoginIp()); vo.setLocation(entity.getLocation()); vo.setBrowser(entity.getBrowser());
        vo.setOs(entity.getOs()); vo.setDeviceType(entity.getDeviceType()); vo.setUserAgent(entity.getUserAgent());
        vo.setStatus(entity.getStatus()); vo.setMessage(entity.getMessage()); vo.setLoginTime(entity.getLoginTime());
        return vo;
    }

    @Override
    public byte[] exportLoginLogs(LoginLogQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        List<LoginLogListVO> list = loginLogMapper.selectExportList(query, tenantId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("日志编号,用户名,登录方式,登录IP,地点,浏览器,操作系统,状态,消息,登录时间\n");
        for (LoginLogListVO vo : list) {
            csv.append(csv(vo.getId())).append(',')
                    .append(csv(vo.getUsername())).append(',')
                    .append(csv(vo.getLoginType())).append(',')
                    .append(csv(vo.getLoginIp())).append(',')
                    .append(csv(vo.getLocation())).append(',')
                    .append(csv(vo.getBrowser())).append(',')
                    .append(csv(vo.getOs())).append(',')
                    .append(csv(vo.getStatus() != null && vo.getStatus() == 1 ? "成功" : "失败")).append(',')
                    .append(csv(vo.getMessage())).append(',')
                    .append(csv(vo.getLoginTime()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int cleanLoginLogs(int beforeDays) {
        Long tenantId = UserContext.getTenantId();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(beforeDays);
        return loginLogMapper.deleteBeforeTime(tenantId, cutoff);
    }

    @Override
    public void saveLoginLog(String username, Long tenantId, String loginType, String loginIp,
                             String userAgent, int status, String message) {
        try {
            LoginLog entity = new LoginLog();
            entity.setTenantId(tenantId);
            entity.setUsername(username);
            entity.setLoginType(loginType);
            entity.setLoginIp(loginIp);
            entity.setLocation(IpUtil.getLocation(loginIp));
            entity.setStatus(status);
            entity.setMessage(message);
            entity.setUserAgent(userAgent);
            entity.setBrowser(parseSimpleBrowser(userAgent));
            entity.setOs(parseSimpleOs(userAgent));
            entity.setDeviceType(parseDeviceType(userAgent));
            entity.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("Failed to save login log for user: {}", username, e);
        }
    }

    private String parseSimpleBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Edge")) return "Edge";
        return "Unknown";
    }

    private String parseSimpleOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "Mac OS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iOS")) return "iOS";
        return "Unknown";
    }

    private String parseDeviceType(String ua) {
        if (ua == null) return "unknown";
        String lower = ua.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) return "mobile";
        if (lower.contains("ipad") || lower.contains("tablet")) return "tablet";
        return "pc";
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
