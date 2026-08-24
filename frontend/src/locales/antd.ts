/**
 * antd 与 dayjs 的语言联动。
 *
 * 三者必须同步：i18next（自有文案）、antd ConfigProvider（分页「共 X 条」、
 * 日期选择器、表格空态）、dayjs（日期格式化）。漏掉 dayjs 的典型症状是
 * 界面全英文但日期还是「2026年8月17日」。
 */
import type { Locale } from 'antd/es/locale';
import enUS from 'antd/locale/en_US';
import zhCN from 'antd/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import 'dayjs/locale/en';

import { DEFAULT_LOCALE, toAppLocale, type AppLocale } from './config';

const ANTD_LOCALES: Record<AppLocale, Locale> = {
  'zh-CN': zhCN,
  'en-US': enUS,
};

const DAYJS_LOCALES: Record<AppLocale, string> = {
  'zh-CN': 'zh-cn',
  'en-US': 'en',
};

export function getAntdLocale(lng?: string | null): Locale {
  return ANTD_LOCALES[toAppLocale(lng) ?? DEFAULT_LOCALE];
}

export function syncDayjsLocale(lng?: string | null): void {
  dayjs.locale(DAYJS_LOCALES[toAppLocale(lng) ?? DEFAULT_LOCALE]);
}
