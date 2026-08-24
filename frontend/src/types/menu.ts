/** 菜单项接口（侧边栏渲染用） */
export interface MenuItem {
  /** 菜单唯一标识 */
  id: string;
  /** 菜单显示名称 */
  label: string;
  /** 图标名称（Ant Design Icons） */
  icon?: string;
  /** 路由路径 */
  path?: string;
  /** 子菜单 */
  children?: MenuItem[];
  /** 所需权限列表 */
  permissions?: string[];
  /** 是否可见 */
  visible?: boolean;
  /** 排序序号 */
  order?: number;
}

/** 后端登录返回的菜单导航项 */
export interface MenuNavItem {
  id: number;
  parentId: number;
  name: string;
  /**
   * i18n key，由后端从 `permission` / `path` 派生（`menu.system.user` 这样）。
   *
   * **可选而非 `| null`**：后端全局配了 Jackson `NON_NULL`，派生不出 key 时
   * 这个字段整个不出现在 JSON 里，而不是 `null`。类型写成 `| null` 会让
   * strictNullChecks 下拿到 `undefined` 却以为是 `null`，`tsc` 就给不出保护。
   *
   * 渲染一律 `label = t(i18nKey) ?? name`，`undefined` 与缺译文都走回退。
   */
  i18nKey?: string;
  path: string | null;
  component: string | null;
  icon: string | null;
  type: 1 | 2;
  sort: number;
  visible: 1 | 0;
  children: MenuNavItem[];
}

/** 菜单数据 API 响应 */
export interface MenuResponse {
  code: number;
  message: string;
  data: MenuItem[];
}
