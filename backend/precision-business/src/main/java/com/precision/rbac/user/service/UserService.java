package com.precision.rbac.user.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.user.dto.*;
import com.precision.rbac.user.vo.UserDetailVO;
import com.precision.rbac.user.vo.UserListVO;

public interface UserService {

    UserDetailVO create(UserCreateDTO dto);

    void update(Long id, UserUpdateDTO dto);

    void remove(Long id);

    UserDetailVO getDetail(Long id);

    PageResult<UserListVO> list(UserQueryDTO query);

    void resetPassword(Long id, UserPasswordResetDTO dto);

    void updatePassword(UserPasswordUpdateDTO dto);

    void assignRoles(Long id, UserRoleAssignDTO dto);

    void updateStatus(Long id, UserStatusDTO dto);
}
