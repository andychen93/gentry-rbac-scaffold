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
4. **面包屑不需要登记**（原来的 `BREADCRUMB_MAP` 已删）——`AppHeader` 优先按 path
   在 `userStore.menus` 里找节点、取它的 `i18nKey`。只有**非菜单的子页面**
   （如 `system/roles/:id/permissions`）才要在 `locales/{lang}/nav.json` 里补一条
   `navKeyFromPath(path)` 派生出来的 key。
5. 菜单要配图标时，图标名同时出现在 `MenuList.tsx` 的 `iconMap` 和
   `menuMapper.ts` 的 `ICON_NAME_MAP`。
6. **页面文案一律走 i18n**：新建 `src/locales/{zh-CN,en-US}/{module}.json`，
   并把 `{module}` 加进 `locales/index.ts` 的 `NAMESPACES`；组件里
   `const { t } = useTranslation('{module}')`，通用词复用 `common` namespace。

## 约定

- 路由是**动态的**：由后端菜单树驱动，不要在 `App.tsx` 里硬写业务路由
  （非菜单的子页面例外，如 `system/roles/:id/permissions`）
- 双 Layout：路径前缀 `/system`、`/monitor`、`/monitor-center` 属「系统管理」，
  其余属「业务系统」。新业务页面用非系统前缀
- 权限渲染用 `useUserStore().hasPermission('xxx:yyy:zzz')`；整页无权限用 `AccessDenied`
- 服务端数据用 TanStack Query，别塞进 Zustand。Zustand 只放 `userStore` / `layoutStore` 这类全局状态
- 字典值展示用 `DictTag`，部门树选择用 `DeptTreeSelect`；字典下拉 `DictSelect` 已删（Pro 表单/查询组件的 dict 分支是死代码一并移除，需要字典选项时在页面层用 dictApi 拉取塞给 select）
- **改配色只动 `theme/argonColors.ts`**（唯一源头）。antd 侧由 `theme/argonTheme.ts`
  灌进 token；`styles/argon.less` 侧由 `vite.config.ts` + `theme/argonLessVars.ts`
  注入成 `@ps-*` Less 变量。token 覆盖不到的样式才写 `argon.less`，且颜色一律用
  `@ps-*` / `fade(@ps-*, N%)`。对照页 `/dev/style`
- **不要写死颜色**：组件里要色值用 `theme.useToken()` 取语义 token
  （`colorPrimary` / `colorSuccess` / `colorError` / `colorTextSecondary`…），
  纯文字灰阶优先 `<Typography.Text type="secondary">`。
  新增颜色请往 `argonColors.ts` 加键，别就地写 hex
  —— `theme/argonLessVars.test.ts` 会拦住 `argon.less` 里的裸 hex/rgba

## i18n（详见 `doc/design/modules/core/P2-国际化i18n-前端详细设计.md`）

- **不要在 .tsx 里写中文字面量**。文案进 `src/locales/{zh-CN,en-US}/{ns}.json`，
  两边 key 必须完全一致（`locales/locales.test.ts` 会对账）
- key 是**扁平**的（`keySeparator: false`）：`t('form.title.create')` 查的是
  字面量为 `form.title.create` 的那一条，不是三层嵌套
- 菜单名与字典 label **不写在前端**：后端下发 `i18nKey`（由 `permission` /
  `path` / `dict_type+dict_value` 派生），前端拿 `nav` / `dict` namespace 翻
- **拿不到 `t` 的三个位置**要特别小心，兜底值必须挪进组件函数体：
  ① 默认参数（`placeholder = '请选择'` → `placeholder ?? t('placeholder.select')`）
  ② 模块级 `Record` 常量（改成存 key，渲染时翻）
  ③ 箭头函数直接返回 JSX 的 `render`
  漏补兜底会让占位符变空，静态扫描查不出来 —— 这类组件要有「不传该 prop 时
  渲染出默认文案」的用例
- 品牌名取 `t(APP_NAME_KEY)`，译文在 `common.json` 的 `app.name`；
  `config/app.ts` 只留与语言无关的 `APP_INITIAL`

## 测试

Pro 组件与布局组件必须有 Vitest 用例。提交前 `npm test` 与 `npx tsc -b` 都要绿。

E2E 的浏览器语言由 `playwright.config.ts` 锁成 `zh-CN`（**别删**，Chromium 默认
是 en-US，会让断言中文文案的用例集体变红）。要验英文界面就
`test.use({ locale: 'en-US' })` + `addInitScript` 预置 `localStorage.gentry_locale`，
**不要去改账号的 `sys_user.language`** —— 那是三级链里优先级最高的一档，会污染其余用例。
