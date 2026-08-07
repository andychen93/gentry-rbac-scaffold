package com.precision.rbac.log.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.precision.core.common.ErrorCode;
import com.precision.core.common.R;
import com.precision.core.exception.BizException;
import com.precision.core.security.TokenBlacklistService;
import com.precision.rbac.log.annotation.Log;
import com.precision.rbac.log.vo.OnlineUserVO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/online-users")
public class OnlineController {

    /** LOG-009 在线用户列表 */
    @GetMapping
    @SaCheckPermission("system:online:list")
    public R<List<OnlineUserVO>> listOnlineUsers(@RequestParam(required = false) String username) {
        List<OnlineUserVO> result = new ArrayList<>();
        try {
            List<String> sessionIds = StpUtil.searchSessionId("", 0, 100, false);
            for (String sessionId : sessionIds) {
                try {
                    var session = StpUtil.getSessionBySessionId(sessionId);
                    if (session == null) continue;
                    OnlineUserVO vo = new OnlineUserVO();
                    vo.setTokenId(sessionId);
                    Object uid = session.get("userId");
                    if (uid != null) vo.setUserId(((Number) uid).longValue());
                    String sessionUsername = (String) session.get("username");
                    vo.setUsername(sessionUsername);
                    if (username != null && !username.isEmpty() && !username.equals(sessionUsername)) continue;
                    // 从 session 获取完整用户信息
                    vo.setNickname((String) session.get("nickname"));
                    vo.setDeptName((String) session.get("deptName"));
                    Object loginIp = session.get("loginIp");
                    if (loginIp != null) vo.setLoginIp(loginIp.toString());
                    vo.setBrowser((String) session.get("browser"));
                    vo.setOs((String) session.get("os"));
                    vo.setLocation((String) session.get("location"));
                    Object loginTimeObj = session.get("loginTime");
                    if (loginTimeObj instanceof java.time.LocalDateTime) {
                        vo.setLoginTime((java.time.LocalDateTime) loginTimeObj);
                    }
                    result.add(vo);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return R.ok(result);
    }

    /** LOG-010 强制下线 */
    @DeleteMapping("/{tokenId}")
    @SaCheckPermission("system:online:forceLogout")
    @Log(module = "在线用户", type = "OTHER", title = "强制下线")
    public R<Void> forceLogout(@PathVariable String tokenId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        try {
            var session = StpUtil.getSessionBySessionId(tokenId);
            if (session != null) {
                Object uid = session.get("userId");
                if (uid != null && ((Number) uid).longValue() == currentUserId) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "不能强制下线当前登录用户");
                }
            }
        } catch (BizException e) { throw e; } catch (Exception ignored) {}
        // JWT 模式：先把被强制下线用户的所有 Token 加入黑名单，再 kickout
        try {
            var session = StpUtil.getSessionBySessionId(tokenId);
            if (session != null) {
                Object uid = session.get("userId");
                if (uid instanceof Number) {
                    TokenBlacklistService.blacklistAllTokensOfUser(((Number) uid).longValue());
                }
            }
        } catch (Exception ignored) {}
        try { StpUtil.kickoutByTokenValue(tokenId); } catch (Exception ignored) {}
        return R.ok();
    }
}
