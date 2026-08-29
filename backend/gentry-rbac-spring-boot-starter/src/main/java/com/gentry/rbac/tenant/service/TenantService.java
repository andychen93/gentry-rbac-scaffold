package com.gentry.rbac.tenant.service;

import com.gentry.core.common.PageResult;
import com.gentry.rbac.tenant.dto.*;
import com.gentry.rbac.tenant.vo.TenantCreateResultVO;
import com.gentry.rbac.tenant.vo.TenantDetailVO;
import com.gentry.rbac.tenant.vo.TenantListVO;
import com.gentry.rbac.tenant.vo.TenantOptionVO;

import java.util.List;

public interface TenantService {

    PageResult<TenantListVO> list(TenantQueryDTO query);

    TenantDetailVO getDetail(Long id);

    TenantCreateResultVO create(TenantCreateDTO dto);

    void update(Long id, TenantUpdateDTO dto);

    void remove(Long id);

    void updateConfig(Long id, TenantConfigDTO dto);

    void updateStatus(Long id, TenantStatusDTO dto);

    List<TenantOptionVO> listOptions();
}
