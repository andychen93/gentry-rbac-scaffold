/** 系统模式：系统管理 / 应用系统 */
export type SystemMode = 'admin' | 'app';

/** 视图模式：列表视图 / 卡片视图 */
export type ViewMode = 'list' | 'card';

/** 主题模式：亮色 / 暗色 */
export type ThemeMode = 'light' | 'dark';

/** 布局状态接口 */
export interface LayoutState {
  /** 系统模式 */
  systemMode: SystemMode;
  /** 视图模式 */
  viewMode: ViewMode;
  /** 主题模式 */
  theme: ThemeMode;
}
