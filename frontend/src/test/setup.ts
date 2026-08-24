import '@testing-library/jest-dom';

// antd Menu / 弹出层依赖 ResizeObserver 与 matchMedia，jsdom 缺，补桩
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
(globalThis as any).ResizeObserver ||= ResizeObserverStub;
if (!window.matchMedia) {
  window.matchMedia = ((query: string) => ({
    matches: false, media: query, onchange: null,
    addEventListener() {}, removeEventListener() {},
    addListener() {}, removeListener() {},
    dispatchEvent: () => false,
  })) as any;
}

/*
 * i18next 测试夹具。
 *
 * 组件用 t() 取文案后，不初始化 i18next 的话 react-i18next 会打印
 * 「You will need to pass in an i18next instance」并让 t() 原样返回 key，
 * 于是所有断言中文文案的用例都会失败。
 *
 * 这里同步注入**真实的语言包**（不是 mock）：
 * - 断言的是真译文，能顺带发现「key 拼错 / 词条漏加」
 * - 默认 zh-CN，与既有用例的中文断言保持一致
 * - 同步 init（不走 resourcesToBackend 懒加载），避免测试里要 await
 */
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import commonZh from '../locales/zh-CN/common.json';
import navZh from '../locales/zh-CN/nav.json';
import dictZh from '../locales/zh-CN/dict.json';
import commonEn from '../locales/en-US/common.json';
import navEn from '../locales/en-US/nav.json';
import dictEn from '../locales/en-US/dict.json';

if (!i18n.isInitialized) {
  void i18n.use(initReactI18next).init({
    lng: 'zh-CN',
    fallbackLng: 'zh-CN',
    ns: ['common', 'nav', 'dict'],
    defaultNS: 'common',
    // 与 locales/index.ts 保持一致：扁平 key
    keySeparator: false,
    nsSeparator: ':',
    interpolation: { escapeValue: false },
    returnNull: false,
    react: { useSuspense: false },
    resources: {
      'zh-CN': { common: commonZh, nav: navZh, dict: dictZh },
      'en-US': { common: commonEn, nav: navEn, dict: dictEn },
    },
  });
}
