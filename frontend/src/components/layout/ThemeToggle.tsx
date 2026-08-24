import React from 'react';
import { Button, Tooltip } from 'antd';
import { BulbOutlined, BulbFilled } from '@ant-design/icons';
import type { ThemeMode } from '../../types/layout';
import { useTranslation } from 'react-i18next';

interface ThemeToggleProps {
  value: ThemeMode;
  onChange: (theme: ThemeMode) => void;
}

/**
 * ThemeToggle 组件 - 主题切换按钮
 * 亮色模式显示 BulbOutlined，暗色模式显示 BulbFilled
 */
const ThemeToggle: React.FC<ThemeToggleProps> = ({ value, onChange }) => {
  const { t } = useTranslation('common');
  const isDark = value === 'dark';

  return (
    <Tooltip title={isDark ? t('common:theme.toLight') : t('common:theme.toDark')}>
      <Button
        type="text"
        icon={isDark ? <BulbFilled /> : <BulbOutlined />}
        onClick={() => onChange(isDark ? 'light' : 'dark')}
        aria-label={t('common:theme.toggle')}
      />
    </Tooltip>
  );
};

export default React.memo(ThemeToggle);
