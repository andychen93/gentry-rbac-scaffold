import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { buildArgonLessVars } from './src/theme/argonLessVars';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  css: {
    preprocessorOptions: {
      less: {
        // 把 argonColors.ts 的色板作为 @ps-* Less 变量注入每个 .less 文件头部，
        // 这样 argon.less 不必再抄一份 hex。改配色只动 theme/argonColors.ts。
        additionalData: buildArgonLessVars(),
      },
    },
  },
  server: {
    port: 3030,
    proxy: {
      // 后端 API 统一前缀，后端默认端口 9090（见 backend/precision-start/src/main/resources/application.yml）
      '/api': {
        target: 'http://127.0.0.1:9090',
        changeOrigin: true,
      },
    },
  },
});
