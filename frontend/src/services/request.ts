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

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 0) {
      return res;
    }
    // 未登录
    if (res.code === 10003) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = '/login';
      return Promise.reject(new Error('未登录'));
    }
    message.error(res.message || '请求失败');
    return Promise.reject(new Error(res.message));
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = '/login';
    } else {
      message.error('网络异常，请稍后重试');
    }
    return Promise.reject(error);
  }
);

export default request;
