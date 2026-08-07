import { create } from 'zustand';
import { authApi, LoginDTO, LoginVO } from '../services/userApi';
import type { MenuNavItem } from '../types/menu';

const TOKEN_KEY = 'precision_token';

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
}

interface UserStore {
  token: string | null;
  userInfo: UserInfo | null;
  menus: MenuNavItem[];
  isLoggedIn: boolean;

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

  login: async (dto: LoginDTO) => {
    const res = await authApi.login(dto);
    const { token, userInfo } = res.data;
    localStorage.setItem(TOKEN_KEY, token);
    set({ token, userInfo, menus: userInfo.menus || [], isLoggedIn: true });
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
