import axios from 'axios';
import { message } from 'antd';

const TOKEN_KEY = 'precision_token';

const request = axios.create({
  baseURL: '',
  timeout: 15000,
});

// 请求拦截器：自动携带 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 会话失效码：30001 TOKEN_INVALID / 30002 TOKEN_EXPIRED（后端 ErrorCode）
// 权限不足码：30003 PERMISSION_DENIED
const AUTH_FAILURE_CODES = [30001, 30002];

// 会话失效：清 token 并跳登录页（已在 /login 则不重复跳转，避免死循环）
function handleSessionExpired() {
  localStorage.removeItem(TOKEN_KEY);
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login';
  }
}

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 0) {
      return res;
    }
    // 会话失效（极少数以 HTTP 200 + 业务码返回的情况）
    if (AUTH_FAILURE_CODES.includes(res.code)) {
      handleSessionExpired();
      return Promise.reject(new Error(res.message || '未登录'));
    }
    message.error(res.message || '请求失败');
    return Promise.reject(new Error(res.message));
  },
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data;
    const code = body?.code;
    const msg = body?.message;

    // 401 或携带认证失效码 → 会话失效，重新登录
    if (status === 401 || AUTH_FAILURE_CODES.includes(code)) {
      handleSessionExpired();
      return Promise.reject(new Error(msg || '未登录'));
    }
    // 403 权限不足（含 jwt-simple 下 session 丢失被后端判为“空权限”的情况）：
    // 展示后端真实原因，不强制跳登录——避免给“已登录但缺权限”的用户造成登录死循环。
    if (status === 403 || code === 30003) {
      message.error(msg || '权限不足');
      return Promise.reject(new Error(msg || '权限不足'));
    }
    message.error(msg || '网络异常，请稍后重试');
    return Promise.reject(error);
  }
);

export default request;
