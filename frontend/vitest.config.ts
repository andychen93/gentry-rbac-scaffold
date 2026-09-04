import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // 与 vite.config.ts 保持一致；vitest 独立配置文件不继承其 resolve 段
      '@gentry/kit': path.resolve(__dirname, './packages/gentry-kit/src'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    exclude: ['**/node_modules/**', '**/dist/**', '**/e2e/**', '**/playwright-report/**', '**/test-results/**'],
  },
});
