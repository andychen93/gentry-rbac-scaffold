package com.precision.rbac.user.service;

import com.precision.rbac.user.dto.LoginDTO;
import com.precision.rbac.user.vo.LoginVO;
import com.precision.rbac.user.vo.UserInfoVO;

public interface AuthService {

    LoginVO login(LoginDTO dto, String loginIp, String userAgent);

    void logout();

    UserInfoVO getUserInfo();
}
