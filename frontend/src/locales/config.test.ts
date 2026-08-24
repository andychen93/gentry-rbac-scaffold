import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  DEFAULT_LOCALE,
  LOCALE_STORAGE_KEY,
  persistLocale,
  resolveInitialLocale,
  SUPPORTED_LOCALES,
  toAppLocale,
  toBackendLocale,
} from './config';

describe('语言码转换', () => {
  it('toBackendLocale_连字符转下划线', () => {
    expect(toBackendLocale('zh-CN')).toBe('zh_CN');
    expect(toBackendLocale('en-US')).toBe('en_US');
  });

  it('toAppLocale_精确匹配_大小写与分隔符无关', () => {
    expect(toAppLocale('zh-CN')).toBe('zh-CN');
    expect(toAppLocale('zh_CN')).toBe('zh-CN');
    expect(toAppLocale('ZH-cn')).toBe('zh-CN');
    expect(toAppLocale('  en_US  ')).toBe('en-US');
  });

  it('toAppLocale_仅语言码_退化匹配', () => {
    // 浏览器可能只发 "en"，只做精确匹配会落空。
    // 这条必须与后端 GentryLocaleResolver.matchHeader 的退化逻辑对称，
    // 否则会出现「界面英文而错误提示中文」
    expect(toAppLocale('en')).toBe('en-US');
    expect(toAppLocale('zh')).toBe('zh-CN');
  });

  it('toAppLocale_zhTW_退化命中zhCN', () => {
    // 有意为之：繁体用户看简体优于看英文
    expect(toAppLocale('zh-TW')).toBe('zh-CN');
    expect(toAppLocale('zh-Hant')).toBe('zh-CN');
  });

  it('toAppLocale_不支持或空值_返回null', () => {
    expect(toAppLocale('ja-JP')).toBeNull();
    expect(toAppLocale('ko')).toBeNull();
    expect(toAppLocale(null)).toBeNull();
    expect(toAppLocale(undefined)).toBeNull();
    expect(toAppLocale('')).toBeNull();
    expect(toAppLocale('   ')).toBeNull();
  });

  it('SUPPORTED_LOCALES_与默认语言自洽', () => {
    expect(SUPPORTED_LOCALES).toContain(DEFAULT_LOCALE);
  });
});

describe('resolveInitialLocale', () => {
  const originalLanguage = navigator.language;

  function stubNavigatorLanguage(value: string) {
    Object.defineProperty(navigator, 'language', { value, configurable: true });
  }

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    stubNavigatorLanguage(originalLanguage);
    vi.restoreAllMocks();
  });

  it('localStorage有值_优先使用', () => {
    localStorage.setItem(LOCALE_STORAGE_KEY, 'en-US');
    stubNavigatorLanguage('zh-CN');
    expect(resolveInitialLocale()).toBe('en-US');
  });

  it('localStorage为空_跟随浏览器语言', () => {
    stubNavigatorLanguage('en-GB');
    expect(resolveInitialLocale()).toBe('en-US');
  });

  it('localStorage为不支持语言_忽略并降级到浏览器', () => {
    localStorage.setItem(LOCALE_STORAGE_KEY, 'ja-JP');
    stubNavigatorLanguage('en-US');
    expect(resolveInitialLocale()).toBe('en-US');
  });

  it('两者都不可用_返回默认', () => {
    stubNavigatorLanguage('ko-KR');
    expect(resolveInitialLocale()).toBe(DEFAULT_LOCALE);
  });

  it('localStorage抛异常_不崩且降级', () => {
    // 隐私模式下 localStorage.getItem 可能抛异常
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('SecurityError');
    });
    stubNavigatorLanguage('en-US');
    expect(resolveInitialLocale()).toBe('en-US');
  });

  it('persistLocale抛异常_不向外抛', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceeded');
    });
    expect(() => persistLocale('en-US')).not.toThrow();
  });
});
