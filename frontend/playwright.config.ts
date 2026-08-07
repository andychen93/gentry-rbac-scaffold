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
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
});
