# Gentry 平台化改造 — 前端实施计划（P4）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把前端共享层（Pro 组件 / 主题 / 分页 hook / API 类型）抽为单仓 workspace 包 `@gentry/kit`，狗粮应用改为消费它，消除跨项目组件拷贝，并为微前端演进保留菜单注册表契约。

**Architecture:** `frontend/package.json` 开 `workspaces: ["packages/*"]`；`packages/gentry-kit` 为**源码包**（不构建产物，`main`/`types` 直指 `src/index.ts`，由消费方 vite/tsc 编译）；应用侧 tsconfig include + vite/vitest 双 alias 解析 `@gentry/kit`。`services/request.ts`、`stores/*`、`menuMapper.ts`、`locales/*` 留应用层（request 依赖应用 i18n 实例；设计文档 §5.1）。

**Tech Stack:** React 18 / TS 5.6 / Vite 6 / antd 5 / Vitest + Testing Library / Playwright

**工作目录：** 所有路径相对 `frontend/`。所有 npm 命令在 `frontend/` 下执行。

**关键背景（零上下文工程师必读）：**

- 现状配置形态（已核实）：
  - `package.json`：无 workspaces 字段，name=gentry-frontend，private=true；
  - `tsconfig.json`：**单工程**（无 references/composite，`include: ["src"]`，noEmit）；
  - `vitest.config.ts`：**独立文件**（非 vite.config 内嵌），无 include 字段（默认 glob 覆盖一切非 exclude 的 `*.test.*`），jsdom 全局，setupFiles=`./src/test/setup.ts`；
  - `vite.config.ts`：alias 只有 `@ → ./src`，less 的 additionalData 用 `buildArgonLessVars()`（现在 import 自 `./src/theme/argonLessVars`）。
- **必须先做的解耦**（grep 已核实，pro 组件有三处隐式依赖应用层，不先解耦搬不动）：
  1. `RowActions.tsx:3` import `../../stores/userStore`（只用了 `hasPermission`）；userStore 拖 `services/userApi → request → locales`。**页面侧 `perm` 用法全部保留**，解法是 RowActions 支持外部注入权限判断。
  2. `QueryForm.tsx:4` / `CrudFormModal.tsx:3` import `../common/DictSelect`；DictSelect 拖 `services/dictApi + locales/navLabel`。**`type: 'dict'` 分支是死代码**：全仓 `type: 'dict'` 字段使用为 0（页面全用 `type: 'select'`/`'node'`），直接删分支。
  3. `RowActions.test.tsx:5` import userStore —— 测试改用注入的 `can`。
- 涉及面实测数字：pro 组件应用侧消费 **12 个非测试文件**；`types/api` 应用侧真实消费 **5 处**（MenuTableSelect / UserTableSelect / StylePreviewPage / OnlineUserPage / notificationApi），其余 6 处 import 在随迁文件内部；theme 下游：`main.tsx`（argonTheme）、`components/common/StatCard.tsx`（argonGradients）、`vite.config.ts`、`styles/argon.less`（零 @import，不受影响）。
- **颜色守卫** `theme/argonLessVars.test.ts` 硬路径引 `../styles/argon.less`（应用侧文件）→ 该测试**留在应用侧**不随迁；`argonColors.test.ts`/`argonTheme.test.ts` 随迁。
- 测试基线：`npm test` 111 绿 + `tsc -b` 干净 + `vite build` 成功 + E2E 15 spec 90 用例（需前后端起着；e2e helpers 只读 `src/locales`，locales 不迁，不受影响）。
- Git 分支：`feature/gentry-platform`（worktree `.claude/worktrees/gentry-platform`）。

**总验证门槛（每个 Task 完成后跑）：**

```bash
cd frontend && npm test && npx tsc -b
```

（动了样式链的 Task 3 另加 `npx vite build`。）

---

### Task 0: 解耦 pro 组件（搬不动的三根线，先剪断）

**Files:**
- Modify: `src/components/pro/RowActions.tsx`（`hasPermission` 改 props 注入 + 保持向后兼容的 Context 方案）
- Modify: `src/components/pro/RowActions.test.tsx`（测试改注入）
- Modify: `src/components/pro/QueryForm.tsx`（删 `'dict'` 类型分支与 DictSelect import）
- Modify: `src/components/pro/CrudFormModal.tsx`（同上）
- Modify: `src/components/pro/QueryForm.test.tsx`、`CrudFormModal.test.tsx`（若有 dict 分支用例，删或改 select）
- Create: `src/components/pro/permission.ts`（权限判断注入点）

- [ ] **Step 1: 写失败的 RowActions 注入测试**

在 `RowActions.test.tsx` 增加用例（现有 import userStore 的用例本步骤一并改掉）：

```tsx
// 顶部 import 改：删 useUserStore，加
import { PermissionProvider } from './permission';

it('can 注入_false 时_perm 项不渲染', () => {
  render(
    <PermissionProvider can={() => false}>
      <RowActions items={[
        { key: 'edit', label: '编辑', icon: <EditOutlined />, perm: 'system:user:edit', onClick: () => {} },
        { key: 'del', label: '删除', icon: <DeleteOutlined />, onClick: () => {} },
      ]} />,
    </PermissionProvider>,
  );
  expect(screen.getByLabelText('删除')).toBeInTheDocument();
  expect(screen.queryByLabelText('编辑')).toBeNull();
});
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd frontend && npx vitest run src/components/pro/RowActions.test.tsx
```

预期：失败（`./permission` 不存在）。

- [ ] **Step 3: 实现 permission.ts + 改 RowActions**

`src/components/pro/permission.ts`：

```ts
import { createContext, useContext } from 'react';

/**
 * 权限判断注入点。kit 不持有登录态（userStore 是应用层的东西），
 * 应用在根组件包 <PermissionProvider can={useUserStore(s => s.hasPermission)}>。
 * 不包 Provider 时默认放行（保持「无权限体系也能用 Pro 组件」）。
 */
const PermissionContext = createContext<(perm: string) => boolean>(() => true);

export const PermissionProvider = PermissionContext.Provider;
export const usePermission = () => useContext(PermissionContext);
```

`RowActions.tsx` 改动：

```tsx
// 删：import { useUserStore } from '../../stores/userStore';
// 加：import { usePermission } from './permission';
// 32 行改：
const can = usePermission();
const visible = items.filter((i) => !i.perm || can(i.perm));
```

- [ ] **Step 4: 删 QueryForm/CrudFormModal 的 dict 死分支**

两个文件：删 `import DictSelect from '../common/DictSelect';`；类型联合里删 `'dict'`；删 `dictType?: string;` 字段；删 `f.type === 'dict'` 渲染分支。跑 `grep -rn "'dict'" src/components/pro` 确认只剩测试文件（有 dict 用例则同步删）。**这是公开 API 收窄，死代码已核实零使用，安全。**

- [ ] **Step 5: 应用侧接 Provider**

`src/App.tsx`（或 `main.tsx` 中 ConfigProvider 内层）：

```tsx
import { PermissionProvider } from './components/pro/permission';
import { useUserStore } from './stores/userStore';

function PermissionGate({ children }: { children: React.ReactNode }) {
  const hasPermission = useUserStore((s) => s.hasPermission);
  return <PermissionProvider can={hasPermission}>{children}</PermissionProvider>;
}
// 包在路由外层（ConfigProvider 内）
```

- [ ] **Step 6: 全量验证**

```bash
cd frontend && npm test && npx tsc -b
```

预期：全绿（RowActions 现有 userStore 用例已改注入；若其余页面测试因 Provider 缺失 perm 行为变化——默认放行，与现状 userStore 未登录时 hasPermission 行为对齐确认）。

- [ ] **Step 7: Commit**

```bash
git add -A frontend/src
git commit -m "refactor(pro): RowActions 权限注入化，删 dict 死分支（kit 抽取前置解耦）"
```

### Task 1: workspace 骨架 + kit 包壳

**Files:**
- Modify: `frontend/package.json`（`"workspaces": ["packages/*"]` + dependencies 加 `"@gentry/kit": "*"`）
- Create: `frontend/packages/gentry-kit/package.json`（含 peerDependencies）
- Create: `frontend/packages/gentry-kit/src/index.ts`（临时空 barrel）
- Modify: `frontend/tsconfig.json`（**include 改 `["src", "packages/gentry-kit/src"]`**；paths 加 `@gentry/kit`）
- Modify: `frontend/vite.config.ts` + `frontend/vitest.config.ts`（**两份都**加 alias）

- [ ] **Step 1: 开 workspace + kit 包声明**

`frontend/package.json` 顶层加 `"workspaces": ["packages/*"],`；dependencies 加 `"@gentry/kit": "*"`。

`frontend/packages/gentry-kit/package.json`：

```json
{
    "name": "@gentry/kit",
    "version": "0.1.0",
    "private": true,
    "main": "src/index.ts",
    "types": "src/index.ts",
    "peerDependencies": {
        "@ant-design/icons": "^5.6.1",
        "@tanstack/react-query": "^5.62.0",
        "antd": "^5.24.0",
        "react": "^18.3.1",
        "react-dom": "^18.3.1",
        "react-i18next": "^15.1.3"
    }
}
```

（源码包：无 build、无产物。peerDependencies 与根版本对齐——hoisting 下狗粮无感，第二个消费项目装包时 npm 会校验。）

- [ ] **Step 2: tsconfig 单工程直改（不建 kit 独立 tsconfig、不搞 references）**

根 `tsconfig.json`：`include` 改 `["src", "packages/gentry-kit/src"]`；paths 加：

```json
"@gentry/kit": ["packages/gentry-kit/src/index.ts"],
"@gentry/kit/*": ["packages/gentry-kit/src/*"]
```

（理由：根是 noEmit 单工程，kit 的 .test 文件今天在 src 就被检查，include 扩围后覆盖不变，无回退。composite/references 与 noEmit 冲突，不引入。）

- [ ] **Step 3: vite + vitest 双 alias（两份配置都要）**

`vite.config.ts` alias 加：

```ts
'@gentry/kit': path.resolve(__dirname, './packages/gentry-kit/src'),
```

`vitest.config.ts` **同样加**（它不继承 vite.config 的 resolve）：

```ts
import path from 'node:path';
// defineConfig 里加
resolve: {
  alias: {
    '@gentry/kit': path.resolve(__dirname, './packages/gentry-kit/src'),
  },
},
```

- [ ] **Step 4: `npm install` + 验证**

```bash
cd frontend && npm install && npm test && npx tsc -b
```

预期：111 全绿（还没有文件 import kit）。**注意：vitest 不需要动 include**——现配置无 include 字段，默认 glob 天然覆盖 `packages/` 下测试；**千万别**把 include 写成只有 `packages/**`，那会把 src 全部测试挤出运行（假绿）。

- [ ] **Step 5: Commit**

```bash
git add -A frontend
git commit -m "chore(frontend): 开 npm workspace，建 @gentry/kit 包壳（vite/vitest 双 alias）"
```

### Task 2: 迁入 types/api + hooks/usePagedList（最小垂直切片）

**Files:**
- Move: `src/types/api.ts` → `packages/gentry-kit/src/types/api.ts`（`api.test.ts` 随迁）
- Move: `src/hooks/usePagedList.ts` → `packages/gentry-kit/src/hooks/usePagedList.ts`（test 随迁）
- Modify: `packages/gentry-kit/src/index.ts`
- Modify: 应用侧 5 处 `types/api` 导入（MenuTableSelect / UserTableSelect / StylePreviewPage / OnlineUserPage / notificationApi）+ PageSelect.tsx 内部引用改相对

- [ ] **Step 1: git mv**

```bash
cd frontend
mkdir -p packages/gentry-kit/src/types packages/gentry-kit/src/hooks
git mv src/types/api.ts packages/gentry-kit/src/types/api.ts
git mv src/types/api.test.ts packages/gentry-kit/src/types/api.test.ts
git mv src/hooks/usePagedList.ts packages/gentry-kit/src/hooks/usePagedList.ts
git mv src/hooks/usePagedList.test.ts packages/gentry-kit/src/hooks/usePagedList.test.ts
```

- [ ] **Step 2: index.ts 导出**（先 `grep "^export" packages/gentry-kit/src/types/api.ts` 对齐实际导出名）

```ts
// @gentry/kit —— 平台共享层（组件/主题/hook/类型），源码包，由消费方编译
export type { ApiResult, PageResult, PageQuery } from './types/api';
export { usePagedList } from './hooks/usePagedList';
```

- [ ] **Step 3: 改应用侧导入（只圈 src，不碰 packages）**

**sed 只对 `src` 跑**（kit 内部互引保持相对路径，`usePagedList.ts` 里的 `from '../types/api'` 不动）：

```bash
cd frontend
grep -rln "types/api'" src --include="*.ts" --include="*.tsx" | xargs sed -i '' "s|from '[^']*types/api'|from '@gentry/kit'|g"
grep -rln "hooks/usePagedList" src --include="*.ts" --include="*.tsx" | xargs sed -i '' "s|from '[^']*hooks/usePagedList'|from '@gentry/kit'|g"
grep -rn "types/api'\|hooks/usePagedList" src   # 期望零命中
```

（`[^']*` 通配任意相对前缀，覆盖 ../、../../ 等所有深度形态。）

- [ ] **Step 4: 验证**

```bash
cd frontend && npm test && npx tsc -b
```

预期：全绿（随迁的 api.test / usePagedList.test 被 vitest 默认 glob 覆盖，tsc include 已扩围）。

- [ ] **Step 5: Commit**

```bash
git add -A frontend
git commit -m "refactor(kit): types/api 与 usePagedList 迁入 @gentry/kit"
```

### Task 3: 迁入 theme/（颜色守卫留应用侧）

**Files:**
- Move: `src/theme/{argonColors,argonTheme,argonLessVars}.ts` + `argonColors.test.ts` + `argonTheme.test.ts` → `packages/gentry-kit/src/theme/`
- **不动**：`src/theme/argonLessVars.test.ts` 留在 `src/theme/`（守卫的是应用侧 `src/styles/argon.less`，硬路径 `path.resolve(__dirname, '../styles/argon.less')` 只有在应用侧才成立）
- Modify: `vite.config.ts`（import 改 `./packages/gentry-kit/src/theme/argonLessVars`）
- Modify: `src/main.tsx`（argonTheme → `@gentry/kit`）、`src/components/common/StatCard.tsx`（argonGradients → `@gentry/kit`）
- Modify: `packages/gentry-kit/src/theme/argonLessVars.ts` + `argonColors.ts` 里生成注释中的旧路径字样（`src/theme/...` → 包内路径）
- Modify: `packages/gentry-kit/src/index.ts`

- [ ] **Step 1: git mv（挑着搬，守卫留下）**

```bash
cd frontend
mkdir -p packages/gentry-kit/src/theme
git mv src/theme/argonColors.ts packages/gentry-kit/src/theme/
git mv src/theme/argonTheme.ts packages/gentry-kit/src/theme/
git mv src/theme/argonLessVars.ts packages/gentry-kit/src/theme/
git mv src/theme/argonColors.test.ts packages/gentry-kit/src/theme/
git mv src/theme/argonTheme.test.ts packages/gentry-kit/src/theme/
# src/theme/ 只剩 argonLessVars.test.ts
```

- [ ] **Step 2: kit index.ts 追加导出**

```ts
export { argonColors, argonGradients } from './theme/argonColors';
export { argonTheme } from './theme/argonTheme';
export { buildArgonLessVars } from './theme/argonLessVars';
```

- [ ] **Step 3: 三处应用侧引用改写**

`vite.config.ts`（node 侧文件，用相对路径）：

```ts
import { buildArgonLessVars } from './packages/gentry-kit/src/theme/argonLessVars';
```

`src/main.tsx`：`import { argonTheme } from '@gentry/kit';`
`src/components/common/StatCard.tsx`：`import { argonGradients } from '@gentry/kit';`
（`src/components/pro/SweetAlert.tsx` 的 argonGradients 引用等 Task 4 随组件目录一起走，此刻不动。）

守卫测试 import 路径修正 —— `src/theme/argonLessVars.test.ts` 顶部若 import 被搬走的源文件（先 grep），改成 `from '@gentry/kit'`。

- [ ] **Step 4: 验证（含构建 + 产物比对）**

```bash
cd frontend && npm test && npx tsc -b && npx vite build
```

预期：全绿 + 构建成功。**产物比对**（CLAUDE.md 的守卫流程）：改造前先 `npx vite build` 抓 `dist/assets/*.css` 里 primary 色值（如 `#5e72e4`）留底，改造后再抓一次 diff —— `@ps-*` 注入值只取决于 argonColors 内容，位置无关，必须逐字节一致。

- [ ] **Step 5: Commit**

```bash
git add -A frontend
git commit -m "refactor(kit): theme 迁入 @gentry/kit，颜色守卫留应用侧守 argon.less"
```

### Task 4: 迁入 components/pro/（含测试）

**Files:**
- Move: `src/components/pro/` 整目录 → `packages/gentry-kit/src/components/pro/`（含 permission.ts、7 组件、index.ts、测试；`DictSelect` 相关 import 已在 Task 0 删除）
- Modify: 应用侧 12 个非测试文件的 pro 导入 → `@gentry/kit`
- Modify: `packages/gentry-kit/src/index.ts`（并入 pro barrel 内容）

- [ ] **Step 1: git mv（先建父目录）**

```bash
cd frontend
mkdir -p packages/gentry-kit/src/components
git mv src/components/pro packages/gentry-kit/src/components/pro
```

- [ ] **Step 2: kit index.ts 并入 pro barrel**

把 `packages/gentry-kit/src/components/pro/index.ts` 的全部 export 行并进 kit 根 index.ts，**追加导出注入点**：

```ts
export { PermissionProvider, usePermission } from './components/pro/permission';
```

原 pro/index.ts 删除（kit 根 barrel 即唯一入口）。

- [ ] **Step 3: 应用侧批量改导入（只圈 src）**

```bash
cd frontend
grep -rln "components/pro" src --include="*.ts" --include="*.tsx" | xargs sed -i '' "s|from '[^']*components/pro'|from '@gentry/kit'|g"
grep -rn "components/pro" src   # 期望零命中（App.tsx 的 permission import 也被上一条覆盖——它 from './components/pro/permission'，[^']* 会命中；若路径形态特殊残留，手工清）
```

kit 内部互引（ProTable→QueryForm、PageSelect→hooks 等）改相对：`../hooks/usePagedList`、`../../types/api`（git mv 后目录层级没变，**其实不用改**——pro 从 `src/components/pro/` 到 `packages/gentry-kit/src/components/pro/`，`../../types/api` 解析到 `packages/gentry-kit/src/types/api` 正好存在，`../hooks/...` 同理。验证门会兜底）。

- [ ] **Step 4: 验证**

```bash
cd frontend && npm test && npx tsc -b && npx vite build
```

预期：全绿。若 kit 内相对路径报错，按报错把 `../../types/api` 等改为 kit 内正确相对层级。

- [ ] **Step 5: Commit**

```bash
git add -A frontend
git commit -m "refactor(kit): Pro 组件全套迁入 @gentry/kit（权限注入点随行）"
```

### Task 5: E2E 终验 + i18n 防线扩围 + 文档收口

- [ ] **Step 1: noHardcodedText 防线扩到 kit**

`src/locales/noHardcodedText.test.ts` 的扫描根 `const SRC = path.join(process.cwd(), 'src')` 扩为同时扫 `src` 与 `packages/gentry-kit/src`（pro 组件是 `t()` 重度用户，迁出后不扩围就是防线漏洞；已核对 ALLOW 豁免清单里的路径都不随迁，相对逻辑不变）：

```ts
const SRC_ROOTS = ['src', 'packages/gentry-kit/src'].map((p) => path.join(process.cwd(), p));
```

（遍历逻辑同步改为多根；跑 `npx vitest run src/locales/noHardcodedText.test.ts` 确认仍绿。）

- [ ] **Step 2: E2E（前后端起着）**

```bash
cd frontend && npm run test:e2e
```

预期：90 用例全绿（按 DOM/label 断言；个别引用旧路径按报错修）。

- [ ] **Step 3: `/dev/style` 对照页人工过一眼**（颜色链换了源头，肉眼回归）

- [ ] **Step 4: 文档收口**

CLAUDE.md / AGENTS.md / 两份指南里的旧路径更新（grep 模式**去掉 `src/` 前缀**再扫，否则漏掉 `components/pro/RowActions.tsx`、`theme/argonColors.ts` 这类无前缀写法——已实测 CLAUDE.md 18/67/82 行、AGENTS.md 326 行都是无前缀形态）：

```bash
grep -rn "theme/argon\|components/pro\|hooks/usePagedList\|types/api" ../CLAUDE.md ../AGENTS.md ../doc/guide/*.md
```

逐条改为 `@gentry/kit` 或 `packages/gentry-kit/src/...` 新路径。CLAUDE.md 里 `frontend/src/theme/argonColors.ts` 唯一源头表述改为 `frontend/packages/gentry-kit/src/theme/argonColors.ts`；颜色守卫的描述补一句「守卫测试留在应用侧 `src/theme/argonLessVars.test.ts`」。

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs(frontend): 共享层入 kit 后的文档路径收口，i18n 防线扩围"
```

---

## 完成判据（对照概要设计 §6 P4）

1. `npm test`（111+）+ `npx tsc -b` + `npx vite build` 全绿；
2. E2E 90 用例全绿；
3. `src/` 下不再有 `components/pro/`、`hooks/usePagedList*`、`types/api*`、`theme/argonColors|argonTheme|argonLessVars.ts`（`src/theme/` 只剩守卫测试 `argonLessVars.test.ts`）；
4. 应用代码对 kit 的引用统一走 `@gentry/kit`（src 内零相对路径指向 packages/，零 `components/pro`/`types/api` 旧引用）；
5. noHardcodedText 防线覆盖 kit 源码；
6. 文档零旧路径残留（无前缀 grep 验证）。
