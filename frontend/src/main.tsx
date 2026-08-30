import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import { useTranslation } from 'react-i18next';
import { argonTheme } from '@gentry/kit';
import { getAntdLocale } from './locales/antd';
import { setupI18n } from './locales';
import './styles/argon.less';
import App from './App';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
});

/**
 * 把 ConfigProvider 包进组件，才能让 antd 的 locale 跟着 i18next 变。
 * 原来是 `<ConfigProvider locale={zhCN}>` 写死在模块顶层，切语言时 antd
 * 组件内置文案（分页「共 X 条」、日期选择器、表格空态）不会更新。
 */
function Root() {
  const { i18n } = useTranslation();
  return (
    <ConfigProvider locale={getAntdLocale(i18n.language)} theme={argonTheme}>
      <AntdApp>
        <App />
      </AntdApp>
    </ConfigProvider>
  );
}

// 必须 await：i18next 未就绪就渲染会闪一下原文
setupI18n().then(() => {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <BrowserRouter>
        <QueryClientProvider client={queryClient}>
          <Root />
        </QueryClientProvider>
      </BrowserRouter>
    </React.StrictMode>
  );
});
