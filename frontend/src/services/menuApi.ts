import request from './request';
import type { MenuItem, MenuResponse } from '../types/menu';

// ========== 菜单管理 VO/DTO 类型 ==========

export interface MenuTreeVO {
  id: number;
  parentId: number;
  name: string;
  icon: string | null;
  type: number;
  sort: number;
  permission: string | null;
  path: string | null;
  component: string | null;
  visible: number;
  status: number;
  isExternal: number;
  isCache: number;
  createTime: string;
  children: MenuTreeVO[];
}

export interface MenuVO {
  id: number;
  parentId: number;
  name: string;
  icon: string | null;
  type: number;
  sort: number;
  permission: string | null;
  path: string | null;
  component: string | null;
  visible: number;
  status: number;
  isExternal: number;
  isCache: number;
  createTime: string;
}

export interface MenuQueryDTO {
  name?: string;
  status?: number;
  type?: number;
}

export interface MenuCreateDTO {
  parentId: number;
  name: string;
  icon?: string;
  type: number;
  sort: number;
  permission?: string;
  path?: string;
  component?: string;
  visible?: number;
  status?: number;
  isExternal?: number;
  isCache?: number;
}

export interface MenuUpdateDTO {
  parentId?: number;
  name: string;
  icon?: string;
  sort: number;
  permission?: string;
  path?: string;
  component?: string;
  visible?: number;
  status?: number;
  isExternal?: number;
  isCache?: number;
}

/** 菜单数据缓存（侧边栏用） */
let menuCache: MenuItem[] | null = null;

/** 菜单 API 服务 */
export const menuApi = {
  /** 获取当前用户的菜单列表（侧边栏用） */
  getMenus: async (): Promise<MenuResponse> => {
    if (menuCache) {
      return { code: 0, message: 'ok', data: menuCache };
    }
    const res = await request.get<any, MenuResponse>('/api/menus');
    menuCache = res.data;
    return res;
  },

  /** 根据系统模式获取菜单（侧边栏用） */
  getMenusByMode: async (mode: string): Promise<MenuResponse> => {
    const res = await request.get<any, MenuResponse>('/api/menus', {
      params: { mode },
    });
    menuCache = res.data;
    return res;
  },

  /** 清除菜单缓存 */
  clearCache: () => {
    menuCache = null;
  },

  // ========== 菜单管理 CRUD 接口 ==========

  /** MENU-001 菜单树查询 */
  tree: (params?: MenuQueryDTO) =>
    request.get<any, { code: number; data: MenuTreeVO[] }>('/api/v1/menus', { params }),

  /** MENU-002 菜单详情 */
  detail: (id: number) =>
    request.get<any, { code: number; data: MenuTreeVO }>(`/api/v1/menus/${id}`),

  /** MENU-003 新增菜单 */
  create: (data: MenuCreateDTO) =>
    request.post<any, { code: number; data: MenuVO }>('/api/v1/menus', data),

  /** MENU-004 编辑菜单 */
  update: (id: number, data: MenuUpdateDTO) =>
    request.put(`/api/v1/menus/${id}`, data),

  /** MENU-005 删除菜单 */
  remove: (id: number) =>
    request.delete(`/api/v1/menus/${id}`),
};
