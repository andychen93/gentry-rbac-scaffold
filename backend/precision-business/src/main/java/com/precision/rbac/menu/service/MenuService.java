package com.precision.rbac.menu.service;

import com.precision.rbac.menu.dto.MenuCreateDTO;
import com.precision.rbac.menu.dto.MenuQueryDTO;
import com.precision.rbac.menu.dto.MenuUpdateDTO;
import com.precision.rbac.menu.vo.MenuTreeVO;
import com.precision.rbac.menu.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 查询菜单树
     */
    List<MenuTreeVO> tree(MenuQueryDTO query);

    /**
     * 查询菜单详情
     */
    MenuTreeVO getDetail(Long id);

    /**
     * 新增菜单
     */
    MenuVO create(MenuCreateDTO dto);

    /**
     * 编辑菜单
     */
    void update(Long id, MenuUpdateDTO dto);

    /**
     * 删除菜单（级联删除子菜单 + 清除角色关联）
     */
    void remove(Long id);
}
