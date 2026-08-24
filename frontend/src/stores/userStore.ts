import { create } from 'zustand';
import { applyServerLocale } from '../locales';
import { authApi, LoginDTO, LoginVO } from '../services/userApi';
import type { MenuNavItem } from '../types/menu';

const TOKEN_KEY = 'gentry_token';

interface UserInfo {
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  deptId: number;
  deptName: string;
  roles: { id: number; roleCode: string; roleName: string; dataScope: number }[];
  permissions: string[];
  menus: MenuNavItem[];
  /** 语言偏好。缺失 = 从未选过 = 跟随浏览器（后端 Jackson NON_NULL 会省略 null） */
  language?: string;
}

interface UserStore {
  token: string | null;
  userInfo: UserInfo | null;
  menus: MenuNavItem[];
  isLoggedIn: boolean;
  passwordExpired: boolean;

  login: (dto: LoginDTO) => Promise<void>;
  logout: () => Promise<void>;
  fetchUserInfo: () => Promise<void>;
  hasPermission: (perm: string) => boolean;
  clearAuth: () => void;
}

export const useUserStore = create<UserStore>((set, get) => ({
  token: localStorage.getItem(TOKEN_KEY),
  userInfo: null,
  menus: [],
  isLoggedIn: !!localStorage.getItem(TOKEN_KEY),
  passwordExpired: false,

  login: async (dto: LoginDTO) => {
    const res = await authApi.login(dto);
    const { token, userInfo, passwordExpired } = res.data;
    localStorage.setItem(TOKEN_KEY, token);
    /*
     * menus 只存**原始数据**（{ name, i18nKey, ... }），绝不在这里把 label 算好。
     * 预算 label 会让切换语言时菜单不更新 —— Soybean Admin 为此不得不写一个
     * updateLocaleOfGlobalMenus() 遍历整树重算；React 里只要渲染时才 t()，
     * 语言变化天然响应式，那个函数根本不需要。改这里前先想清楚这一点。
     */
    set({ token, userInfo, menus: userInfo.menus || [], isLoggedIn: true, passwordExpired: !!passwordExpired });
    // 服务端偏好优先于浏览器语言，在跳首页之前切好，避免界面闪一下
    await applyServerLocale(userInfo.language);
  },

  logout: async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore
    }
    localStorage.removeItem(TOKEN_KEY);
    set({ token: null, userInfo: null, menus: [], isLoggedIn: false });
  },

  fetchUserInfo: async () => {
    const res = await authApi.getUserInfo();
    set({ userInfo: res.data, menus: res.data.menus || [] });
    // 刷新页面时同样以服务端偏好为准
    await applyServerLocale(res.data.language);
  },

  hasPermission: (perm: string) => {
    const { userInfo } = get();
    if (!userInfo) return false;
    return userInfo.permissions.includes(perm);
  },

  clearAuth: () => {
    localStorage.removeItem(TOKEN_KEY);
    set({ token: null, userInfo: null, menus: [], isLoggedIn: false });
  },
}));
