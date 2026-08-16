---
inclusion: fileMatch
fileMatchPattern: 'frontend/**/*.{ts,tsx}'
---

# 前端编码约定（改 TS/TSX 时生效）

权威约定在 [AGENTS.md](../../AGENTS.md) 第八章。

## 新增页面的完整动作

1. `src/pages/{module}/{Xxx}Page.tsx` —— 列表页用 `ProTable`，
   增删改用 `CrudFormModal`，行操作用 `RowActions`，状态切换用 `StatusSwitch`。
   最简样例：`src/pages/dept/`。
2. `src/services/{module}Api.ts` —— 用 `services/request.ts` 封装，
   返回类型写成 `{ code: number; data: T }`（拦截器已剥一层 axios response）。
3. **`src/utils/menuMapper.ts` 的 `COMPONENT_MAP` 登记页面** —— 漏了菜单点不开，
   key 必须与 `sys_menu.component` 的值一致。
4. `src/components/layout/AppHeader.tsx` 的 `BREADCRUMB_MAP` 加面包屑名称。
5. 菜单要配图标时，图标名同时出现在 `MenuList.tsx` 的 `iconMap` 和
   `menuMapper.ts` 的 `ICON_NAME_MAP`。

## 约定

- 路由是**动态的**：由后端菜单树驱动，不要在 `App.tsx` 里硬写业务路由
  （非菜单的子页面例外，如 `system/roles/:id/permissions`）
- 双 Layout：路径前缀 `/system`、`/monitor`、`/monitor-center` 属「系统管理」，
  其余属「业务系统」。新业务页面用非系统前缀
- 权限渲染用 `useUserStore().hasPermission('xxx:yyy:zzz')`；整页无权限用 `AccessDenied`
- 服务端数据用 TanStack Query，别塞进 Zustand。Zustand 只放 `userStore` / `layoutStore` 这类全局状态
- 字典值展示用 `DictTag`，字典下拉用 `DictSelect`，部门树选择用 `DeptTreeSelect`
- **改配色只动 `theme/argonColors.ts`**（唯一源头）。antd 侧由 `theme/argonTheme.ts`
  灌进 token；`styles/argon.less` 侧由 `vite.config.ts` + `theme/argonLessVars.ts`
  注入成 `@ps-*` Less 变量。token 覆盖不到的样式才写 `argon.less`，且颜色一律用
  `@ps-*` / `fade(@ps-*, N%)`。对照页 `/dev/style`
- **不要写死颜色**：组件里要色值用 `theme.useToken()` 取语义 token
  （`colorPrimary` / `colorSuccess` / `colorError` / `colorTextSecondary`…），
  纯文字灰阶优先 `<Typography.Text type="secondary">`。
  新增颜色请往 `argonColors.ts` 加键，别就地写 hex
  —— `theme/argonLessVars.test.ts` 会拦住 `argon.less` 里的裸 hex/rgba

## 测试

Pro 组件与布局组件必须有 Vitest 用例。提交前 `npm test` 与 `npx tsc -b` 都要绿。
