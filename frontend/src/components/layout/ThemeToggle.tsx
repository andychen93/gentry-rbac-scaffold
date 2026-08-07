import React from 'react';
import { Button, Tooltip } from 'antd';
import { BulbOutlined, BulbFilled } from '@ant-design/icons';
import type { ThemeMode } from '../../types/layout';

interface ThemeToggleProps {
  value: ThemeMode;
  onChange: (theme: ThemeMode) => void;
}

/**
 * ThemeToggle 组件 - 主题切换按钮
 * 亮色模式显示 BulbOutlined，暗色模式显示 BulbFilled
 */
const ThemeToggle: React.FC<ThemeToggleProps> = ({ value, onChange }) => {
  const isDark = value === 'dark';

  return (
    <Tooltip title={isDark ? '切换亮色模式' : '切换暗色模式'}>
      <Button
        type="text"
        icon={isDark ? <BulbFilled /> : <BulbOutlined />}
        onClick={() => onChange(isDark ? 'light' : 'dark')}
        aria-label="切换主题"
      />
    </Tooltip>
  );
};

export default React.memo(ThemeToggle);
