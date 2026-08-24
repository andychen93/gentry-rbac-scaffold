import type { TFunction } from 'i18next';

/**
 * 系统内置字典的枚举值清单。
 *
 * **为什么需要这个文件**：页面里散落着字典内容的硬编码拷贝，而且已经漂移了 ——
 * `UserFormModal` 的职务下拉只列了 6 个（库里 `sys_user_post` 有 8 个），
 * `UserPage` 的状态筛选写「正常 / 禁用」（库里 `sys_normal_disable` 是「正常 / 停用」）。
 *
 * 这里只存**值**，文案统一走 `dict` namespace 里后端派生的 key
 * （`dict.{dictType}.{dictValue}`，见 `DictI18nKeyResolver`）。于是：
 * - 硬编码的中文消失，i18n 免费获得
 * - 字典内容只有一份译文来源，漂移由构造消除
 * - 不引入 API 依赖（`DictSelect` 会走接口，这里是纯静态枚举）
 *
 * 需要「租户可自行增删的字典」时用 `DictSelect` / `DictTag`；
 * 这里只服务**系统枚举**（代码里也在依赖其取值，如 status 的 0/1）。
 */

export const DICT_TYPES = {
  gender: 'sys_user_gender',
  normalDisable: 'sys_normal_disable',
  userPost: 'sys_user_post',
  dataScope: 'sys_data_scope',
  menuType: 'sys_menu_type',
} as const;

/** 各字典的值清单，顺序即下拉展示顺序 */
export const DICT_VALUES = {
  [DICT_TYPES.gender]: ['0', '1', '2'],
  [DICT_TYPES.normalDisable]: ['1', '0'],
  [DICT_TYPES.userPost]: [
    'CEO', 'Director', 'Manager', 'Supervisor',
    'ChiefArchitect', 'SeniorEngineer', 'Engineer', 'Driver',
  ],
  [DICT_TYPES.dataScope]: ['1', '2', '3', '4', '5'],
  [DICT_TYPES.menuType]: ['1', '2', '3'],
} as const;

/** 取单个字典项的译文。key 与后端 DictI18nKeyResolver 的派生规则一致 */
export function dictLabel(t: TFunction, dictType: string, value: string | number): string {
  return t(`dict.${dictType}.${value}`, { ns: 'dict', defaultValue: String(value) });
}

/** 生成 antd Select/Radio 的 options */
export function dictOptions(
  t: TFunction,
  dictType: keyof typeof DICT_VALUES,
  opts: { numeric?: boolean } = {},
): { value: string | number; label: string }[] {
  return DICT_VALUES[dictType].map((v) => ({
    value: opts.numeric ? Number(v) : v,
    label: dictLabel(t, dictType, v),
  }));
}
