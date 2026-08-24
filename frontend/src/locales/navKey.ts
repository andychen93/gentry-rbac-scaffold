import type { MenuNavItem } from '../types/menu';

/**
 * 由路由 path 派生导航 key，规则与后端 `MenuI18nKeyResolver` 的 path 分支保持一致。
 *
 * `/system/users` → `menu.system.users`
 *
 * 只在**菜单树里找不到该 path** 时才用（非菜单的子页面，如
 * `system/roles/:id/permissions`）。菜单树里能找到的一律直接取节点自己的
 * `i18nKey` —— 那是后端算的，权威。
 *
 * 根路径 `/` 返回空串：它派生不出有意义的 key（会得到裸 `menu`），而 `/` 只是
 * App 依据菜单重定向前的中转站，本来就不该有面包屑。不特判的话开发期控制台会被
 * 「missing key: nav:menu」刷屏。
 */
export function navKeyFromPath(path: string): string {
  const normalized = path.replace(/\/+$/, '').replace(/\/+/g, '.');
  return normalized ? 'menu' + normalized : '';
}

/** 在菜单树里按 path 深度查找节点 */
export function findMenuByPath(
  menus: MenuNavItem[],
  path: string,
): MenuNavItem | undefined {
  for (const m of menus) {
    if (m.path === path) return m;
    if (m.children?.length) {
      const hit = findMenuByPath(m.children, path);
      if (hit) return hit;
    }
  }
  return undefined;
}
