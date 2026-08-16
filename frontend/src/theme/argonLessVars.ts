import { argonColors, argonGradients, argonShadow, argonShadowLg } from './argonColors';

/**
 * 把 TS 色板编译成 Less 变量声明，由 vite.config.ts 的
 * `css.preprocessorOptions.less.additionalData` 注入到每个 .less 文件头部。
 *
 * 为什么走这条路，而不是在 argon.less 里再抄一份色板：
 *   抄一份就有两个源头，改配色要改两处，迟早漂移。这里让 argonColors.ts
 *   保持唯一源头，Less 侧的变量在编译期由它生成，物理上不可能不同步。
 *
 * 为什么用 Less 变量而不是 CSS 自定义属性（var(--ps-primary)）：
 *   1. Less 变量在**编译期**解析，写错名字直接编译失败；CSS 变量写错只是
 *      静默失效（该条声明作废、颜色退回继承值），线上才发现。
 *   2. 编译产物与手写 hex 完全一致，不引入运行时求值，也没有首屏
 *      变量未注入的闪色问题。
 * 代价是无法在运行时切换主题 —— 真要做暗色模式，那时再往 :root 铺一层
 * CSS 变量，本文件的角色不变（依然由 argonColors.ts 生成）。
 */
export function buildArgonLessVars(): string {
  const lines = [
    '// ⚠️ 由 src/theme/argonLessVars.ts 自动注入，不要在 .less 里改这些值',
    '// 改配色请动 src/theme/argonColors.ts',
  ];

  // 基础色：@ps-primary / @ps-gray700 / @ps-white …（键名直接用 argonColors 的键）
  for (const [key, value] of Object.entries(argonColors)) {
    lines.push(`@ps-${key}: ${value};`);
  }

  // 渐变：@ps-gradient-primary …（Argon 的 87deg adjust-hue 公式，Less 侧算不出来）
  for (const [key, value] of Object.entries(argonGradients)) {
    lines.push(`@ps-gradient-${key}: ${value};`);
  }

  lines.push(`@ps-shadow: ${argonShadow};`);
  lines.push(`@ps-shadow-lg: ${argonShadowLg};`);

  return lines.join('\n');
}
