import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
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
