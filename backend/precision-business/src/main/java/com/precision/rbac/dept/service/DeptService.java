package com.precision.rbac.dept.service;

import com.precision.rbac.dept.dto.DeptCreateDTO;
import com.precision.rbac.dept.dto.DeptQueryDTO;
import com.precision.rbac.dept.dto.DeptUpdateDTO;
import com.precision.rbac.dept.vo.DeptTreeVO;
import com.precision.rbac.dept.vo.DeptVO;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DeptService {

    /**
     * 创建默认部门（租户编排时调用）
     */
    Long createDefaultDept(Long tenantId, String name);

    /**
     * 查询部门树
     */
    List<DeptTreeVO> tree(DeptQueryDTO query);

    /**
     * 查询部门详情
     */
    DeptTreeVO getDetail(Long id);

    /**
     * 新增部门
     */
    DeptVO create(DeptCreateDTO dto);

    /**
     * 编辑部门
     */
    void update(Long id, DeptUpdateDTO dto);

    /**
     * 删除部门
     */
    void remove(Long id);

    /**
     * 获取指定部门及其所有子部门的 ID 列表
     */
    List<Long> getChildDeptIds(Long deptId);
}
