import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { buildArgonLessVars } from './packages/gentry-kit/src/theme/argonLessVars';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@gentry/kit': path.resolve(__dirname, './packages/gentry-kit/src'),
    },
  },
  css: {
    preprocessorOptions: {
      less: {
        // 把 argonColors.ts 的色板作为 @ps-* Less 变量注入每个 .less 文件头部，
        // 这样 argon.less 不必再抄一份 hex。改配色只动 @gentry/kit 的 theme/argonColors.ts。
        additionalData: buildArgonLessVars(),
      },
    },
  },
  server: {
    port: 3030,
    /*
     * 监听所有网卡，使前端可用局域网地址访问（如 http://172.20.10.3:3030）。
     * 这是「日志里能看到真实 IP」的前提：用 localhost 访问时，浏览器到 dev server
     * 走回环，客户端地址本来就是 127.0.0.1，服务端无从得知你的局域网地址。
     * 注意这会把 dev server 暴露到局域网，仅开发环境使用。
     */
    host: true,
    proxy: {
      // 后端 API 统一前缀，后端默认端口 9090（见 backend/gentry-start/src/main/resources/application.yml）
      '/api': {
        target: 'http://127.0.0.1:9090',
        changeOrigin: true,
        /*
         * 转发客户端真实 IP。不开这个的话，后端 request.getRemoteAddr() 拿到的是
         * dev server 自己的连接地址（恒为 127.0.0.1），操作日志/登录日志/在线用户
         * 里的 IP 全变成 127.0.0.1。开启后 http-proxy 会补上
         * X-Forwarded-For / X-Forwarded-Host / X-Forwarded-Proto，
         * 后端 IpUtil.getClientIp() 优先读 XFF。
         *
         * 注意：用 localhost/127.0.0.1 访问前端时，浏览器到 dev server 走的是回环，
         * 客户端地址「本来就是」127.0.0.1，这时记到 127.0.0.1 是正确结果。
         * 想看到局域网 IP，请用局域网地址访问前端（如 http://172.20.10.3:3030）。
         */
        xfwd: true,
      },
    },
  },
});
