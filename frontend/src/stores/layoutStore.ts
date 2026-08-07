import { create } from 'zustand';
import type { SystemMode, ViewMode, ThemeMode } from '../types/layout';
import type { MenuItem } from '../types/menu';

/** 本地存储 key 常量 */
export const LAYOUT_SIDEBAR_PINNED_KEY = 'precision_sidebar_pinned';
export const LAYOUT_SYSTEM_MODE_KEY = 'precision_system_mode';
export const LAYOUT_VIEW_MODE_KEY = 'precision_view_mode';
export const LAYOUT_THEME_KEY = 'precision_theme';

/** Layout Store 接口 */
interface LayoutStore {
  // 状态
  sidebarPinned: boolean;
  systemMode: SystemMode;
  viewMode: ViewMode;
  theme: ThemeMode;
  menuItems: MenuItem[];
  menuLoading: boolean;
  menuError: string | null;

  // Actions
  setSidebarPinned: (pinned: boolean) => void;
  toggleSidebarPinned: () => void;
  setSystemMode: (mode: SystemMode) => void;
  setViewMode: (mode: ViewMode) => void;
  setTheme: (theme: ThemeMode) => void;
  setMenuItems: (items: MenuItem[]) => void;
  setMenuLoading: (loading: boolean) => void;
  setMenuError: (error: string | null) => void;
  initializeLayout: () => void;
}

export const useLayoutStore = create<LayoutStore>((set) => ({
  sidebarPinned: true,
  systemMode: 'admin',
  viewMode: 'list',
  theme: 'light',
  menuItems: [],
  menuLoading: false,
  menuError: null,

  setSidebarPinned: (pinned: boolean) => {
    localStorage.setItem(LAYOUT_SIDEBAR_PINNED_KEY, JSON.stringify(pinned));
    set({ sidebarPinned: pinned });
  },

  toggleSidebarPinned: () => {
    const next = !useLayoutStore.getState().sidebarPinned;
    localStorage.setItem(LAYOUT_SIDEBAR_PINNED_KEY, JSON.stringify(next));
    set({ sidebarPinned: next });
  },

  setSystemMode: (mode: SystemMode) => {
    localStorage.setItem(LAYOUT_SYSTEM_MODE_KEY, mode);
    set({ systemMode: mode });
  },

  setViewMode: (mode: ViewMode) => {
    localStorage.setItem(LAYOUT_VIEW_MODE_KEY, mode);
    set({ viewMode: mode });
  },

  setTheme: (theme: ThemeMode) => {
    localStorage.setItem(LAYOUT_THEME_KEY, theme);
    set({ theme });
  },

  setMenuItems: (items: MenuItem[]) => {
    set({ menuItems: items });
  },

  setMenuLoading: (loading: boolean) => {
    set({ menuLoading: loading });
  },

  setMenuError: (error: string | null) => {
    set({ menuError: error });
  },

  initializeLayout: () => {
    const pinned = localStorage.getItem(LAYOUT_SIDEBAR_PINNED_KEY);
    const systemMode = localStorage.getItem(LAYOUT_SYSTEM_MODE_KEY);
    const viewMode = localStorage.getItem(LAYOUT_VIEW_MODE_KEY);
    const theme = localStorage.getItem(LAYOUT_THEME_KEY);

    set({
      sidebarPinned: pinned ? JSON.parse(pinned) : true,
      systemMode: (systemMode as SystemMode) || 'admin',
      viewMode: (viewMode as ViewMode) || 'list',
      theme: (theme as ThemeMode) || 'light',
    });
  },
}));
