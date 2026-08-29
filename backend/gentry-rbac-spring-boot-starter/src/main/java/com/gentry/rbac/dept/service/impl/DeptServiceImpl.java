package com.gentry.rbac.dept.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.exception.BizException;
import com.gentry.core.security.UserContext;
import com.gentry.rbac.dept.dto.DeptCreateDTO;
import com.gentry.rbac.dept.dto.DeptQueryDTO;
import com.gentry.rbac.dept.dto.DeptUpdateDTO;
import com.gentry.rbac.dept.entity.Dept;
import com.gentry.rbac.dept.mapper.DeptMapper;
import com.gentry.rbac.dept.service.DeptService;
import com.gentry.rbac.dept.vo.DeptTreeVO;
import com.gentry.rbac.dept.vo.DeptVO;
import com.gentry.rbac.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Service
public class DeptServiceImpl implements DeptService {

    private static final Logger log = LoggerFactory.getLogger(DeptServiceImpl.class);
    private static final int MAX_DEPTH = 5;

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    public DeptServiceImpl(DeptMapper deptMapper, UserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Long createDefaultDept(Long tenantId, String name) {
        Dept dept = new Dept();
        dept.setTenantId(tenantId);
        dept.setParentId(0L);
        dept.setAncestors("0");
        dept.setName(name);
        dept.setSort(0);
        dept.setStatus(1);
        deptMapper.insert(dept);
        log.info("Created default dept: id={}, tenantId={}, name={}", dept.getId(), tenantId, name);
        return dept.getId();
    }

    @Override
    public List<DeptTreeVO> tree(DeptQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        List<Dept> deptList = deptMapper.selectList(query, tenantId);

        // 批量查询用户数
        Map<Long, Integer> userCountMap = buildUserCountMap(tenantId);

        // 转换为 VO
        List<DeptTreeVO> voList = deptList.stream()
                .map(d -> toTreeVO(d, userCountMap))
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(voList);
    }

    @Override
    public DeptTreeVO getDetail(Long id) {
        Dept dept = getExistingDept(id);
        Long tenantId = UserContext.getTenantId();
        Map<Long, Integer> userCountMap = buildUserCountMap(tenantId);
        return toTreeVO(dept, userCountMap);
    }

    @Override
    @Transactional
    public DeptVO create(DeptCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;

        // 校验部门名称租户内唯一
        if (deptMapper.countByName(tenantId, dto.getName(), null) > 0) {
            throw new BizException(ErrorCode.DEPT_NAME_EXISTS);
        }

        // 计算 ancestors 并校验层级
        String ancestors;
        if (parentId == 0L) {
            ancestors = "0";
        } else {
            Dept parentDept = getExistingDept(parentId);
            ancestors = parentDept.getAncestors() + "," + parentId;
        }

        // 校验层级不超过 5 级（ancestors 中逗号数量 + 1 = 当前层级）
        int depth = ancestors.split(",").length;
        if (depth >= MAX_DEPTH) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.depth.exceeded");
        }

        // 查询负责人姓名（反范式）
        String leaderName = null;
        if (dto.getLeaderId() != null) {
            var user = userMapper.selectOneById(dto.getLeaderId());
            if (user != null) {
                leaderName = user.getNickname();
            }
        }

        Dept dept = new Dept();
        dept.setParentId(parentId);
        dept.setAncestors(ancestors);
        dept.setName(dto.getName());
        dept.setLeaderId(dto.getLeaderId());
        dept.setLeaderName(leaderName);
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setSort(dto.getSort());
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        deptMapper.insert(dept);

        log.info("Created dept: id={}, name={}", dept.getId(), dept.getName());
        return toVO(dept);
    }

    @Override
    @Transactional
    public void update(Long id, DeptUpdateDTO dto) {
        Dept existingDept = getExistingDept(id);
        Long tenantId = UserContext.getTenantId();
        Long newParentId = dto.getParentId() != null ? dto.getParentId() : existingDept.getParentId();

        // 校验部门名称租户内唯一（排除自身）
        if (deptMapper.countByName(tenantId, dto.getName(), id) > 0) {
            throw new BizException(ErrorCode.DEPT_NAME_EXISTS);
        }

        // 如果修改了上级部门
        boolean parentChanged = !newParentId.equals(existingDept.getParentId());
        String newAncestors = existingDept.getAncestors();

        if (parentChanged) {
            // 校验不能移动到自己的子部门下
            if (newParentId.equals(id)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.cannot.move.into.descendant");
            }
            if (newParentId != 0L) {
                Dept newParent = getExistingDept(newParentId);
                // 检查新父级的 ancestors 是否包含当前部门 ID
                String newParentAncestors = newParent.getAncestors();
                if (newParentAncestors.contains("," + id + ",")
                        || newParentAncestors.endsWith("," + id)
                        || newParentAncestors.equals(String.valueOf(id))) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.cannot.move.into.descendant");
                }
                newAncestors = newParentAncestors + "," + newParentId;
            } else {
                newAncestors = "0";
            }

            // 校验层级不超过 5 级
            int depth = newAncestors.split(",").length;
            if (depth >= MAX_DEPTH) {
                throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.depth.exceeded");
            }

            // 递归更新子孙部门的 ancestors
            String oldAncestorsPrefix = existingDept.getAncestors() + "," + id;
            String newAncestorsPrefix = newAncestors + "," + id;
            deptMapper.updateChildrenAncestors(oldAncestorsPrefix, newAncestorsPrefix);
        }

        // 查询负责人姓名
        String leaderName = existingDept.getLeaderName();
        if (dto.getLeaderId() != null && !dto.getLeaderId().equals(existingDept.getLeaderId())) {
            var user = userMapper.selectOneById(dto.getLeaderId());
            leaderName = user != null ? user.getNickname() : null;
        }

        Dept dept = new Dept();
        dept.setId(id);
        dept.setParentId(newParentId);
        dept.setAncestors(newAncestors);
        dept.setName(dto.getName());
        dept.setLeaderId(dto.getLeaderId());
        dept.setLeaderName(leaderName);
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setSort(dto.getSort());
        dept.setStatus(dto.getStatus());
        deptMapper.update(dept);

        log.info("Updated dept: id={}, name={}", id, dto.getName());
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getExistingDept(id);

        // 校验部门下无用户
        int userCount = deptMapper.countUsersByDeptId(id);
        if (userCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.has.users");
        }

        // 校验部门下无子部门
        int childCount = deptMapper.countByParentId(id);
        if (childCount > 0) {
            throw new BizException(ErrorCode.DEPT_HAS_CHILDREN);
        }

        deptMapper.logicDeleteById(id);
        log.info("Deleted dept: id={}", id);
    }

    @Override
    public List<Long> getChildDeptIds(Long deptId) {
        Dept dept = deptMapper.selectOneById(deptId);
        if (dept == null) return List.of(deptId);
        String ancestorsPrefix = dept.getAncestors() + "," + deptId;
        List<Dept> children = deptMapper.selectByAncestorsLike(ancestorsPrefix);
        List<Long> ids = new ArrayList<>();
        ids.add(deptId);
        for (Dept child : children) {
            ids.add(child.getId());
        }
        return ids;
    }

    // ========== 私有方法 ==========

    private Dept getExistingDept(Long id) {
        Dept dept = deptMapper.selectOneById(id);
        if (dept == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.dept.not.found");
        }
        return dept;
    }

    private Map<Long, Integer> buildUserCountMap(Long tenantId) {
        List<Map<String, Object>> counts = deptMapper.countUsersByTenantGroupByDept(tenantId);
        Map<Long, Integer> map = new HashMap<>();
        if (counts != null) {
            for (Map<String, Object> row : counts) {
                Object deptIdObj = row.get("deptid");
                Object cntObj = row.get("cnt");
                if (deptIdObj != null && cntObj != null) {
                    map.put(((Number) deptIdObj).longValue(), ((Number) cntObj).intValue());
                }
            }
        }
        return map;
    }

    private List<DeptTreeVO> buildTree(List<DeptTreeVO> voList) {
        Map<Long, List<DeptTreeVO>> childrenMap = voList.stream()
                .collect(Collectors.groupingBy(DeptTreeVO::getParentId));

        List<DeptTreeVO> roots = new ArrayList<>();
        for (DeptTreeVO vo : voList) {
            vo.setChildren(childrenMap.getOrDefault(vo.getId(), new ArrayList<>()));
            if (vo.getParentId() == 0L) {
                roots.add(vo);
            }
        }
        return roots;
    }

    private DeptTreeVO toTreeVO(Dept dept, Map<Long, Integer> userCountMap) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setName(dept.getName());
        vo.setLeaderId(dept.getLeaderId());
        vo.setLeaderName(dept.getLeaderName());
        // phone 字段脱敏
        vo.setPhone(maskPhone(dept.getPhone()));
        vo.setEmail(dept.getEmail());
        vo.setSort(dept.getSort());
        vo.setStatus(dept.getStatus());
        vo.setUserCount(userCountMap.getOrDefault(dept.getId(), 0));
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }

    private DeptVO toVO(Dept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setAncestors(dept.getAncestors());
        vo.setName(dept.getName());
        vo.setLeaderId(dept.getLeaderId());
        vo.setLeaderName(dept.getLeaderName());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setSort(dept.getSort());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
