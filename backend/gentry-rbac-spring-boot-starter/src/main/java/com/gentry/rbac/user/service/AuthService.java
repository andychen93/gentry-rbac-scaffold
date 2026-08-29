package com.gentry.rbac.user.service;

import com.gentry.rbac.user.dto.LoginDTO;
import com.gentry.rbac.user.vo.LoginVO;
import com.gentry.rbac.user.vo.UserInfoVO;

public interface AuthService {

    LoginVO login(LoginDTO dto, String loginIp, String userAgent);

    void logout();

    UserInfoVO getUserInfo();
}
