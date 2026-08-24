import { GlobalOutlined } from '@ant-design/icons';
import { Dropdown, type MenuProps } from 'antd';
import { useMemo } from 'react';

import { useLocale } from '../../hooks/useLocale';
import type { AppLocale } from '../../locales/config';

/**
 * 语言的**自称**。语言选择器里显示自称是通行做法 ——
 * 否则英文用户看到「中文」反而不认识。所以这份 label 不参与 i18n，是常量。
 */
const NATIVE_NAMES: Record<AppLocale, string> = {
  'zh-CN': '简体中文',
  'en-US': 'English',
};

/**
 * 语言切换器。
 *
 * 纯前端切换、零 API 往返：菜单树在 store 里存的是原始 `{ name, i18nKey }`，
 * label 在渲染时才算，所以语言一变整个界面（含侧边栏）立即重渲染，
 * 不需要重新请求菜单接口。落库是 fire-and-forget，不阻塞 UI。
 */
export default function LocaleSwitcher() {
  const { locale, setLocale, supported } = useLocale();

  const items: MenuProps['items'] = useMemo(
    () =>
      supported.map((code) => ({
        key: code,
        label: NATIVE_NAMES[code],
      })),
    [supported],
  );

  return (
    <Dropdown
      menu={{
        items,
        selectable: true,
        selectedKeys: [locale],
        onClick: ({ key }) => void setLocale(key as AppLocale),
      }}
      placement="bottomRight"
      trigger={['click']}
    >
      <span
        role="button"
        tabIndex={0}
        aria-label="Switch language"
        data-testid="locale-switcher"
        style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center' }}
      >
        <GlobalOutlined />
      </span>
    </Dropdown>
  );
}
