package com.precision.rbac.role.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.role.dto.*;
import com.precision.rbac.role.vo.RoleDetailVO;
import com.precision.rbac.role.vo.RoleListVO;
import com.precision.rbac.role.vo.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    Long createAdminRole(Long tenantId, String roleCode, String roleName);

    PageResult<RoleListVO> list(RoleQueryDTO query);

    RoleDetailVO getDetail(Long id);

    RoleVO create(RoleCreateDTO dto);

    void update(Long id, RoleUpdateDTO dto);

    void remove(Long id);

    void assignMenus(Long roleId, RoleMenuAssignDTO dto);

    void updateDataScope(Long roleId, RoleDataScopeDTO dto);

    void updateStatus(Long id, RoleStatusDTO dto);

    /**
     * 查看角色关联的用户ID列表
     */
    List<Long> listUserIdsByRoleId(Long roleId);
}
