import React from 'react';
import { APP_NAME, APP_INITIAL } from '../../config/app';

interface LogoProps {
  title?: string;
  collapsed?: boolean;
}

/**
 * Logo 组件 - 显示系统名称和 Logo 图标
 * collapsed 时只显示图标，不显示文字
 * 改名字去 config/app.ts
 */
const Logo: React.FC<LogoProps> = ({ title = APP_NAME, collapsed = false }) => {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, overflow: 'hidden', whiteSpace: 'nowrap' }}>
      <div
        style={{
          width: 32,
          height: 32,
          minWidth: 32,
          background: '#1677ff',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          fontSize: 16,
          fontWeight: 'bold',
        }}
      >
        {APP_INITIAL}
      </div>
      {!collapsed && (
        <span style={{ fontSize: 16, fontWeight: 600, color: '#000000d9' }}>{title}</span>
      )}
    </div>
  );
};

export default React.memo(Logo);
