import type { TFunction } from 'i18next';

/**
 * 能被翻译的导航节点：库里的原始名 + 后端派生的 key。
 *
 * `i18nKey` 允许 `null`：后端 `MenuI18nKeyResolver` 在 permission 与 path 都为空时返回 null
 * （Jackson NON_NULL 通常会省掉该字段，但契约上是可空的）。两种「没有 key」都走 name 兜底。
 */
export interface TranslatableNode {
  name: string;
  i18nKey?: string | null;
}

/**
 * 菜单/字典标签的**唯一**解析规则，全站共用。
 *
 * ```
 * i18nKey 有值且语言包命中 → 译文
 * i18nKey 有值但语言包缺 key → defaultValue，即库里的 name
 * i18nKey 缺失（permission 与 path 都为空，如派生项目新增的菜单）→ name
 * ```
 *
 * 与 Soybean Admin 的 `const label = i18nKey ? $t(i18nKey) : title!` 同构，
 * 额外加了 `defaultValue` 兜底，保证**语言包缺 key 时显示中文而不是裸 key**。
 *
 * 用法上有个硬约束：**返回的函数必须在渲染时调用，结果不能存进 store 或模块级常量**。
 * 预算 label 会让切换语言时菜单不更新——Soybean 为此不得不写
 * `updateLocaleOfGlobalMenus()` 遍历整树重算；React 里只要渲染时才算，天然响应式。
 */
export function makeNavLabel(t: TFunction) {
  return (node: TranslatableNode): string =>
    node.i18nKey ? t(node.i18nKey, { ns: 'nav', defaultValue: node.name }) : node.name;
}

/** 字典标签同理，只是 namespace 换成 dict、兜底字段是 dictLabel */
export function makeDictLabel(t: TFunction) {
  return (node: { dictLabel: string; i18nKey?: string | null }): string =>
    node.i18nKey ? t(node.i18nKey, { ns: 'dict', defaultValue: node.dictLabel }) : node.dictLabel;
}

/**
 * 角色名同理，namespace 换成 role、兜底字段是 roleName。
 *
 * 只对内置角色（ADMIN/USER）真正命中：`RoleI18nKeyResolver` 对任意 roleCode 都会
 * 派生出 key，但自建角色的译文语言包里必然没有，`defaultValue` 会兜底显示 roleName——
 * 这是有意的降级，不是 bug。
 */
export function makeRoleLabel(t: TFunction) {
  return (node: { roleName: string; i18nKey?: string | null }): string =>
    node.i18nKey ? t(node.i18nKey, { ns: 'role', defaultValue: node.roleName }) : node.roleName;
}
