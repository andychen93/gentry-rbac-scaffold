# CLAUDE.md

本仓库的技术宪法在 **[AGENTS.md](./AGENTS.md)**，请先完整读一遍再动手。

配套文档索引在 **[DOC_INDEX.md](./DOC_INDEX.md)**。

## 快速定位

| 我要… | 去哪 |
|-------|------|
| 加一个业务模块（后端） | `doc/guide/RBAC模块开发指南.md`，照抄 `com.gentry.rbac.dept` |
| 用横切组件（限流/数据权限/日志…） | `doc/guide/Core组件开发指南.md` |
| 加一个页面（前端） | `frontend/src/pages/dept/` 是最简样例；页面必须在 `utils/menuMapper.ts` 登记。Pro 组件 / theme / `usePagedList` / `types/api` 一律 `import … from '@gentry/kit'`（workspace 包 `frontend/packages/gentry-kit/`，源码直引不构建），菜单注册 `menuMapper` 仍在应用层 |
| 加权限点 | 写 Flyway 迁移插 `sys_menu` + `sys_role_menu`，权限串与 `@SaCheckPermission` 一致 |
| 改数据库结构 | 双流：平台迁移（V≤999）只能新增 `backend/gentry-rbac-spring-boot-starter/src/main/resources/db/migration/gentry-rbac/common/V{n}__xxx.sql`；项目迁移（V≥1000，狗粮自用）在 `backend/gentry-start/src/main/resources/db/migration/common/V{n}__xxx.sql`。三库通用放 `common/`，方言差异才分 `mysql/postgresql/sqlite` 子目录 |
| 换用的数据库（mysql/postgresql/sqlite） | `bash scripts/dev_up.sh --db=postgresql`，见 `.kiro/steering/database-migration.md` |
| 改配色 / 主题 | `frontend/packages/gentry-kit/src/theme/argonColors.ts`（唯一源头），见下「样式与按钮规范」，对照页 `/dev/style` |
| 加表格行内操作按钮 | `@gentry/kit` 的 `RowActions`（`frontend/packages/gentry-kit/src/components/pro/`），见下「样式与按钮规范」 |
| 起本地环境 | `bash scripts/deps_up.sh && bash scripts/dev_up.sh`（默认 MySQL） |

## 硬约束（违反即 Review 打回）

1. 不写死 `tenant_id`，一律 `UserContext.getTenantId()`
2. 不 `throw new RuntimeException`，一律 `BizException(ErrorCode.XXX)`
3. Controller 不直接调 Mapper
4. 不修改已发布的 Flyway 迁移文件
5. 无详细设计文档不编码
6. **不在 `.tsx` / `.less` 里写死颜色**（hex、`rgb()`、`rgba()`），取色方式见下
7. **表格行内操作一律用 `RowActions`**，纯图标 + Tooltip，不写文字
8. **启用/停用开关必须二次确认**（`StatusSwitch` 的 `confirmText`）
9. **查询/重置按钮一律靠右**，多个工具栏按钮必须用 `<Space>` 包
10. 提交前 `mvn test` + `npm test` + `npx tsc -b` 全绿
    （`mvn test` 需 `-Dspring.datasource.password=<真实密码>`）

---

## 样式与按钮规范

### 一、颜色只有一个源头

`frontend/packages/gentry-kit/src/theme/argonColors.ts`（`@gentry/kit` 的 `theme/argonColors`）
是全站配色的**唯一**出处，两条下游自动跟随，**改配色只动这个文件**：

```
@gentry/kit theme/argonColors.ts ──→ argonTheme.ts ──────→ antd 组件（ConfigProvider token）
                                 └─→ argonLessVars.ts ─┬─→ styles/argon.less（@ps-* Less 变量）
                                                       └ 注入点：vite.config.ts 的
                                                         css.preprocessorOptions.less.additionalData
```

Less 侧的 `@ps-*` 变量是**编译期**注入的，不落盘生成文件，所以不存在「忘了重新生成」。
变量名写错是**构建失败**（`[less] variable @ps-xxx is undefined`），不是静默失效
—— 这也是这里刻意用 Less 变量而不是 CSS 自定义属性（`var(--x)`）的原因：
CSS 变量写错只会让那条声明作废、颜色悄悄退回继承值，线上才发现。

代价：不支持运行时换肤。真要做暗色模式，那时在 `:root` 铺一层 CSS 变量，
`argonLessVars.ts` 的角色不变。

### 二、要用颜色时，按这张表取

| 场景 | 正确写法 | 反例 |
|------|---------|------|
| 纯文字灰阶（说明、占位、次要标签） | `<Typography.Text type="secondary">` | `<span style={{ color: '#999' }}>` |
| 语义化文字（危险、成功） | `<Text type="danger">` / `type="success"` | `style={{ color: '#ff4d4f' }}` |
| 组件里确实要色值（inline style 背景、ECharts option） | `const { token } = theme.useToken()` 取 `colorPrimary` / `colorSuccess` / `colorWarning` / `colorError` / `colorTextSecondary` / `colorBgContainer` … | 写死 hex |
| `.less` 文件里 | `@ps-primary`、`fade(@ps-danger, 8%)` | `#5e72e4`、`rgba(245,54,92,.08)` |
| 渐变（token 表达不了，唯一可 import 的情况） | `import { argonGradients } from '@gentry/kit'`，`.less` 里用 `@ps-gradient-*` | 手写 `linear-gradient(...)` |

补充规则：

- **不要在页面里 `import { argonColors }`**。那会绕开 antd token，往
  `ConfigProvider` 叠覆盖（暗色/租户换肤）时这条硬连线不跟着变，配色会裂。
  渐变是唯一例外（antd 没有渐变 token）。
- **需要新颜色**：往 `argonColors.ts` 加一个键，`@ps-{键名}` 和 antd token 自动可用。
  别就地写死，也别引第二套色板 —— 历史上这里混过 antd 默认色（`#1677ff`/`#ff4d4f`）
  和 Element Plus 色板（`#67C23A`/`#E6A23C`/`#F56C6C`），全是这么来的。
- **改了基础色要同步改 `argonGradients` 对应项**。渐变的第二个色标是独立字面量，
  不是从基础色算出来的（实测与注释里的 `adjust-hue(+25deg)` 公式不一致，
  抄的是 Argon 编译产物），推导会悄悄改掉观感。
- `/dev/style` 对照页**允许**写死 hex —— 它的用途就是跟 Argon 原版逐项比对。

守卫：应用侧 `src/theme/argonLessVars.test.ts` 会拦住 `argon.less` 里的裸 hex 与非黑白
`rgba()`（色板与 `buildArgonLessVars` 本体已迁 `@gentry/kit`，守卫测试留在应用侧守
`argon.less`）。`argon.less` 的 `//` 注释里刻意保留 Argon 原值作为文档，不算违规。

### 三、表格行内操作：`RowActions`

**纯图标 + 悬停气泡**，不带文字。带文字的操作列在窄列里会把中文逐字换行，很丑；
形态也对齐 Argon 官方表格操作列（`.table-action` 本身就是图标 + tooltip）。

```tsx
<RowActions items={[
  { key: 'edit', label: '编辑', icon: <EditOutlined />,
    perm: 'system:user:edit', onClick: () => openEdit(row) },
  { key: 'del',  label: '删除', icon: <DeleteOutlined />, danger: true,
    perm: 'system:user:delete', confirmText: '确认删除该用户？',
    onClick: () => remove(row.id) },
]} />
```

| 字段 | 规则 |
|------|------|
| `icon` | **必填**（TS 强制）。文字已移进 Tooltip，没图标就是个空白按钮 |
| `label` | 一词多用：Tooltip 文案 + `aria-label`（无障碍名）+ E2E 定位锚点（`getByLabel('删除')`）。**别省** |
| `perm` | 无权限直接不渲染该项 |
| `danger` | **只管红色样式**，不产生二次确认 |
| `confirmText` | **有值才包 `Popconfirm`**。`onClick` 内部已有 `Modal.confirm` 的（如 Redis 删除 Key）**不要传**，否则要确认两遍 |

`danger` 与 `confirmText` 是刻意拆开的两件事，不要合并成一个「危险操作」标志位。

### 三之二、状态开关：`StatusSwitch` 必须二次确认

启用/停用是会改变账号能否登录的操作，**必须**传 `confirmText`，文案要点名到具体对象
并说清后果：

```tsx
<StatusSwitch
  id={r.id} status={s} onToggle={handleStatus}
  confirmText={(next) => next
    ? `确定启用用户「${r.username}」？启用后该账号可以正常登录。`
    : `确定停用用户「${r.username}」？停用后该账号将无法登录，已登录的会话不受影响。`}
/>
```

- `confirmText` 不传则点击直接生效（旧行为），新增页面不要图省事省掉。
- `Switch` 受控于 `status`，用户点「取消」时开关不会自己翻过去，**不需要手动回滚 UI**。
- 文案别写「立即踢下线」这种没实现的承诺：`updateStatus` 只改库里的 status，
  不调 `StpUtil.kickout`，已登录会话会继续有效到 Token 过期。

### 三之三、查询区：查询/重置按钮一律靠右

`QueryForm`（`ProTable` 全线复用）已处理好；**自己手写查询表单时**（树形页如
`DeptPage` / `MenuPage` 不走 ProTable）按钮 Col 必须这么写：

```tsx
<Col flex="auto" style={{ textAlign: 'right' }}>
  <Form.Item style={{ marginBottom: 0 }}>
    <Space>
      <Button type="primary" onClick={handleSearch}>查询</Button>
      <Button onClick={handleReset}>重置</Button>
    </Space>
  </Form.Item>
</Col>
```

`flex="auto"` 吃掉本行剩余宽度，`textAlign` 把按钮推到最右；字段刚好占满一行时该 Col
会换行，此时 auto = 整行宽，按钮仍在右端。不要用 `<Col xs={24} sm={12} md={8}>`
那种固定栅格 —— 按钮会紧跟在最后一个字段右边靠左排。

### 三之四、多个工具栏按钮必须用 `<Space>` 包

`ProTable` 的 `toolbar` 只是塞进一个普通 `div`，**不会替你加间距**。裸 `<>` 包两个
按钮，它们之间的间隙是 0px：

```tsx
toolbar={<Space><Button>新增</Button><Button>刷新缓存</Button></Space>}   // ✅
toolbar={<><Button>新增</Button><Button>刷新缓存</Button></>}             // ❌ 贴在一起
```

### 四、按钮与交互细节（改样式前先知道为什么是这样）

- 图标库固定 `@ant-design/icons`，不要引第二套。
- 行内图标热区 **28×28**（WCAG 2.5.8 要求 ≥24×24，Argon 原版只有 14px 不达标）。
- 键盘可达：`<a>` 无 `href` 时默认不可聚焦，`RowActions` 显式加了 `tabIndex={0}`，
  Enter/Space 转发成真实 `click`（直接调 `onClick` 会跳过 `Popconfirm`）。
- 焦点环只在 `:focus-visible` 出现，鼠标点击不留残环。
- **行内小图标不做位移**。`.ant-btn` 的「hover 上移 -1px」质感刻意不套用到操作列
  —— 鼠标横扫一行会让整排图标抖动。
- 页面级按钮：渐变用 `.ps-btn-gradient-{primary|info|success|warning|danger|default}`，
  次要按钮用 `.ps-btn-neutral`。
- 行内操作 hover 用项目既有 hover 语言（`gray100` 浅底 + `primary` 字，
  与 dropdown / pagination 一致）；Argon 原版 `#adb5bd→#919ca6` 几乎看不见，没照抄。

### 五、改完要验的

```bash
cd frontend && npm test && npx tsc -b     # 含颜色守卫用例
cd frontend && npx vite build             # Less 变量写错只有构建能发现
cd frontend && npm run test:e2e           # 动了页面/操作列就要跑（需前后端都起着）
```

改 `argon.less` 这类大范围样式重构，**别只看「构建通过」**：构建和单测都盖不住颜色错。
可行做法是改造前后各编译一次 Less 做全文 diff，逐条确认差异等价：

```bash
npx lessc src/styles/argon.less /tmp/before.css   # 改造前
# …改造…
npx lessc src/styles/argon.less /tmp/after.css && diff /tmp/before.css /tmp/after.css
```

（`argon.less` 现在依赖注入的 `@ps-*` 变量，单独 `lessc` 需先把变量声明拼到文件头部。）
