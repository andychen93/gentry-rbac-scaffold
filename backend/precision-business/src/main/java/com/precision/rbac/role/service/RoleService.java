package com.precision.rbac.role.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.role.dto.*;
import com.precision.rbac.role.vo.RoleDetailVO;
import com.precision.rbac.role.vo.RoleListVO;
import com.precision.rbac.role.vo.RoleOptionVO;
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

    /**
     * 绑定用户（全量覆盖：先解除该角色全部关联，再按 userIds 重建）
     */
    void assignUsers(Long roleId, RoleUserAssignDTO dto);

    /**
     * 当前租户内启用角色的下拉选项
     */
    List<RoleOptionVO> listOptions();
}
