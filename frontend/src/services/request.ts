import axios from 'axios';
import { message } from 'antd';
import i18n from '../locales';
import { DEFAULT_LOCALE } from '../locales/config';

const TOKEN_KEY = 'gentry_token';

const request = axios.create({
  baseURL: '',
  timeout: 15000,
});

// 请求拦截器：自动携带 Token 与语言
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  /*
   * Accept-Language 是**登录前唯一的语言通道** —— 此时后端拿不到 sys_user.language，
   * 只能靠这个头决定「用户名或密码错误」用哪种语言返回。
   *
   * 直接读 i18n.language 而不是 localStorage：用户切过语言但还没刷新页面时，
   * i18n 实例才是当前语言的唯一真源。后端 GentryLocaleResolver 同时接受
   * zh-CN 与 zh_CN 两种写法，这里发 BCP47 原样即可。
   */
  config.headers['Accept-Language'] = i18n.language || DEFAULT_LOCALE;
  return config;
});

/**
 * 裸 `fetch` 用的请求头。
 *
 * 下载 Excel / 上传导入文件必须绕开 axios 拦截器（拦截器会把响应解包成 JSON，
 * 二进制流拿不到），于是那几处走原生 fetch —— 也就绕开了上面的请求拦截器。
 *
 * 抽成唯一真源而不是各处手拼：**漏了 `Accept-Language` 不会报错**，只会让导出的
 * Excel 表头回退成后端默认语言（`export_*.properties` 是按 locale 解析的），
 * 英文用户下载到中文表头，且没有任何报错线索。
 */
export function rawFetchHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Accept-Language': i18n.language || DEFAULT_LOCALE,
  };
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

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
      return Promise.reject(new Error(res.message || i18n.t('err.notLoggedIn')));
    }
    message.error(res.message || i18n.t('err.requestFailed'));
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
      return Promise.reject(new Error(msg || i18n.t('err.notLoggedIn')));
    }
    // 403 权限不足（含 jwt-simple 下 session 丢失被后端判为“空权限”的情况）：
    // 展示后端真实原因，不强制跳登录——避免给“已登录但缺权限”的用户造成登录死循环。
    if (status === 403 || code === 30003) {
      const forbidden = msg || i18n.t('err.forbidden');
      message.error(forbidden);
      return Promise.reject(new Error(forbidden));
    }
    message.error(msg || i18n.t('err.network'));
    return Promise.reject(error);
  }
);

export default request;
