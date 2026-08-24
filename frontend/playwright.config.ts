import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  // 一次性预登录内置账号，避免整套用例打满登录接口限流（10 次/60 秒）
  globalSetup: './e2e/global-setup.ts',
  timeout: 30000,
  expect: { timeout: 5000 },
  fullyParallel: false,  // 串行执行，避免数据冲突
  workers: 1,
  retries: 0,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:3030',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    /*
     * 固定成 zh-CN。**不能省** —— Playwright 起的 Chromium 默认 navigator.language 是
     * en-US，而 resolveInitialLocale() 会跟随浏览器语言，于是整套断言中文文案的用例
     * 会集体变红（表现为「找不到『新增』按钮」这类，很难联想到语言）。
     * 需要验英文界面的用例自己 test.use({ locale: 'en-US' }) 覆盖。
     */
    locale: 'zh-CN',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
});
