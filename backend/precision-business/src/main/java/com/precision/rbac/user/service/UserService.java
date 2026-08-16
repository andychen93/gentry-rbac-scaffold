package com.precision.rbac.user.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.user.dto.*;
import com.precision.rbac.user.vo.UserDetailVO;
import com.precision.rbac.user.vo.UserImportResultVO;
import com.precision.rbac.user.vo.UserListVO;
import com.precision.rbac.user.vo.UserOptionVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserService {

    UserDetailVO create(UserCreateDTO dto);

    void update(Long id, UserUpdateDTO dto);

    void remove(Long id);

    UserDetailVO getDetail(Long id);

    PageResult<UserListVO> list(UserQueryDTO query);

    void resetPassword(Long id, UserPasswordResetDTO dto);

    void updatePassword(UserPasswordUpdateDTO dto);

    /** 修改当前登录用户的个人资料 */
    void updateProfile(UserProfileUpdateDTO dto);

    void assignRoles(Long id, UserRoleAssignDTO dto);

    void updateStatus(Long id, UserStatusDTO dto);

    /** 导出当前查询条件的用户为 Excel */
    void exportUsers(UserQueryDTO query, HttpServletResponse response) throws IOException;

    /** 导入用户（Excel），返回成功/失败统计与错误明细 */
    UserImportResultVO importUsers(MultipartFile file);

    /** 下载用户导入模板 */
    void downloadUserTemplate(HttpServletResponse response) throws IOException;

    /** 当前租户内启用用户的下拉选项（供角色绑定用户的穿梭框） */
    List<UserOptionVO> listOptions();
}
