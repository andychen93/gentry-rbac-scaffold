import React from 'react';
import { theme } from 'antd';
import { useTranslation } from 'react-i18next';
import { APP_NAME_KEY, APP_INITIAL } from '../../config/app';

interface LogoProps {
  title?: string;
  collapsed?: boolean;
}

/**
 * Logo 组件 - 显示系统名称和 Logo 图标
 * collapsed 时只显示图标，不显示文字
 *
 * `title` 的兜底值**不能写在默认参数里** —— 默认参数在组件外求值，那里拿不到 `t`，
 * 也不会随语言切换重算。所以默认 undefined，函数体里再回退到 `app.name`。
 */
const Logo: React.FC<LogoProps> = ({ title, collapsed = false }) => {
  const { t } = useTranslation();
  // 品牌圆标取主色 token（argonTheme → argonColors.primary），
  // 原先写死的 #1677ff 是 antd 默认蓝，和站内其他地方的 Argon 靛不是一个颜色
  const { token } = theme.useToken();

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, overflow: 'hidden', whiteSpace: 'nowrap' }}>
      <div
        style={{
          width: 32,
          height: 32,
          minWidth: 32,
          background: token.colorPrimary,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: token.colorTextLightSolid,
          fontSize: 16,
          fontWeight: 'bold',
        }}
      >
        {APP_INITIAL}
      </div>
      {!collapsed && (
        <span style={{ fontSize: 16, fontWeight: 600, color: token.colorTextHeading }}>
          {title ?? t(APP_NAME_KEY)}
        </span>
      )}
    </div>
  );
};

export default React.memo(Logo);
