import { message } from 'antd';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';

import { syncHtmlLang } from '../locales';
import { syncDayjsLocale } from '../locales/antd';
import {
  DEFAULT_LOCALE,
  persistLocale,
  SUPPORTED_LOCALES,
  toAppLocale,
  toBackendLocale,
  type AppLocale,
} from '../locales/config';
import { userApi } from '../services/userApi';
import { useUserStore } from '../stores/userStore';

/**
 * 语言切换。
 *
 * 刻意**不建 Zustand store**：i18next 实例自身就是语言的唯一真源，`useTranslation()`
 * 已提供订阅与重渲染。再套一层会出现两个真源、需要双向同步。
 */
export function useLocale() {
  const { i18n } = useTranslation();
  const locale = toAppLocale(i18n.language) ?? DEFAULT_LOCALE;

  const setLocale = useCallback(
    async (next: AppLocale) => {
      if (next === locale) return;

      // ① i18next：所有 t() 触发重渲染。菜单也随之变化，因为 store 里存的是
      //    原始 { name, i18nKey }，label 是渲染时才算的
      await i18n.changeLanguage(next);
      // ② dayjs：漏了它的症状是界面全英文但日期还是「2026年8月17日」
      syncDayjsLocale(next);
      //    <html lang>：屏幕阅读器、浏览器翻译提示、:lang() 都依赖它
      syncHtmlLang(next);
      // ③ localStorage：保证下次打开登录页就是这个语言
      persistLocale(next);

      // ④ 落库。fire-and-forget：界面已经切好了，失败只影响下次登录与
      //    导出/定时通知这些后端场景，不该回滚 UI
      if (useUserStore.getState().isLoggedIn) {
        userApi.updateMyLanguage(toBackendLocale(next)).catch(() => {
          // 不写 defaultValue：那会变成同一句话的第二份真源，和语言包漂移时无人发现
          message.warning(i18n.t('common:locale.persistFailed'));
        });
      }
    },
    [locale, i18n]
  );

  return { locale, setLocale, supported: SUPPORTED_LOCALES };
}
