package com.precision.rbac.menu.service.impl;

import com.precision.core.common.ErrorCode;
import com.precision.core.exception.BizException;
import com.precision.rbac.menu.dto.MenuCreateDTO;
import com.precision.rbac.menu.dto.MenuQueryDTO;
import com.precision.rbac.menu.dto.MenuUpdateDTO;
import com.precision.rbac.menu.entity.Menu;
import com.precision.rbac.menu.enums.MenuType;
import com.precision.rbac.menu.mapper.MenuMapper;
import com.precision.rbac.menu.service.MenuService;
import com.precision.rbac.menu.vo.MenuTreeVO;
import com.precision.rbac.menu.vo.MenuVO;
import com.precision.rbac.role.mapper.RoleMenuMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuServiceImpl(MenuMapper menuMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public List<MenuTreeVO> tree(MenuQueryDTO query) {
        List<Menu> menuList = menuMapper.selectList(query);
        List<MenuTreeVO> voList = menuList.stream()
                .map(this::toTreeVO)
                .collect(Collectors.toList());
        return buildTree(voList);
    }

    @Override
    public MenuTreeVO getDetail(Long id) {
        Menu menu = getExistingMenu(id);
        return toTreeVO(menu);
    }

    @Override
    @Transactional
    public MenuVO create(MenuCreateDTO dto) {
        Long parentId = dto.getParentId() != null ? dto.getParentId() : 0L;

        // 按 type 动态校验必填字段
        validateByType(dto.getType(), dto.getPermission(), dto.getPath(), dto.getComponent());

        // 校验同级名称唯一
        if (menuMapper.countByName(parentId, dto.getName(), null) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "同级已存在同名菜单");
        }

        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setName(dto.getName());
        menu.setIcon(dto.getIcon());
        menu.setType(dto.getType());
        menu.setSort(dto.getSort());
        menu.setPermission(dto.getPermission());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setVisible(dto.getVisible() != null ? dto.getVisible() : 1);
        menu.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        menu.setIsExternal(dto.getIsExternal() != null ? dto.getIsExternal() : 0);
        menu.setIsCache(dto.getIsCache() != null ? dto.getIsCache() : 0);
        menuMapper.insert(menu);

        log.info("Created menu: id={}, name={}, type={}", menu.getId(), menu.getName(), menu.getType());
        return toVO(menu);
    }

    @Override
    @Transactional
    public void update(Long id, MenuUpdateDTO dto) {
        Menu existingMenu = getExistingMenu(id);

        // 按已有 type 校验必填字段（type 不可修改）
        validateByType(existingMenu.getType(), dto.getPermission(), dto.getPath(), dto.getComponent());

        Long newParentId = dto.getParentId() != null ? dto.getParentId() : existingMenu.getParentId();

        // 校验不能移动到自己的子菜单下
        if (!newParentId.equals(existingMenu.getParentId()) && newParentId != 0L) {
            if (newParentId.equals(id)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "不能将菜单移动到自己的子菜单下");
            }
            // 递归检查新父级是否是当前菜单的子孙
            Set<Long> childIds = collectChildIds(id);
            if (childIds.contains(newParentId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "不能将菜单移动到自己的子菜单下");
            }
        }

        // 校验同级名称唯一（排除自身）
        if (menuMapper.countByName(newParentId, dto.getName(), id) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "同级已存在同名菜单");
        }

        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(newParentId);
        menu.setName(dto.getName());
        menu.setIcon(dto.getIcon());
        menu.setSort(dto.getSort());
        menu.setPermission(dto.getPermission());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
        menu.setIsExternal(dto.getIsExternal());
        menu.setIsCache(dto.getIsCache());
        menuMapper.update(menu);

        log.info("Updated menu: id={}, name={}", id, dto.getName());
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getExistingMenu(id);

        // 递归收集所有子菜单 ID
        Set<Long> allIds = collectChildIds(id);
        allIds.add(id);

        List<Long> idList = new ArrayList<>(allIds);

        // 批量逻辑删除所有菜单
        menuMapper.batchLogicDelete(idList);

        // 删除 sys_role_menu 中对应的关联记录
        roleMenuMapper.deleteByMenuIds(idList);

        log.info("Deleted menu and children: ids={}", idList);
    }

    // ========== 私有方法 ==========

    private void validateByType(Integer type, String permission, String path, String component) {
        if (type == MenuType.BUTTON.getCode()) {
            if (permission == null || permission.isBlank()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "按钮权限标识不能为空");
            }
        } else if (type == MenuType.MENU.getCode()) {
            if (path == null || path.isBlank()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "菜单路由地址不能为空");
            }
            if (component == null || component.isBlank()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "菜单组件路径不能为空");
            }
        }
    }

    /**
     * 递归收集所有子菜单 ID
     */
    private Set<Long> collectChildIds(Long parentId) {
        Set<Long> result = new HashSet<>();
        List<Menu> children = menuMapper.selectByParentId(parentId);
        for (Menu child : children) {
            result.add(child.getId());
            result.addAll(collectChildIds(child.getId()));
        }
        return result;
    }

    private Menu getExistingMenu(Long id) {
        Menu menu = menuMapper.selectOneById(id);
        if (menu == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "菜单不存在");
        }
        return menu;
    }

    private List<MenuTreeVO> buildTree(List<MenuTreeVO> voList) {
        Map<Long, List<MenuTreeVO>> childrenMap = voList.stream()
                .collect(Collectors.groupingBy(MenuTreeVO::getParentId));

        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO vo : voList) {
            vo.setChildren(childrenMap.getOrDefault(vo.getId(), new ArrayList<>()));
            if (vo.getParentId() == 0L) {
                roots.add(vo);
            }
        }
        return roots;
    }

    private MenuTreeVO toTreeVO(Menu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setIcon(menu.getIcon());
        vo.setType(menu.getType());
        vo.setSort(menu.getSort());
        vo.setPermission(menu.getPermission());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setIsExternal(menu.getIsExternal());
        vo.setIsCache(menu.getIsCache());
        vo.setCreateTime(menu.getCreateTime());
        return vo;
    }

    private MenuVO toVO(Menu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setIcon(menu.getIcon());
        vo.setType(menu.getType());
        vo.setSort(menu.getSort());
        vo.setPermission(menu.getPermission());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setVisible(menu.getVisible());
        vo.setStatus(menu.getStatus());
        vo.setIsExternal(menu.getIsExternal());
        vo.setIsCache(menu.getIsCache());
        vo.setCreateTime(menu.getCreateTime());
        return vo;
    }
}
