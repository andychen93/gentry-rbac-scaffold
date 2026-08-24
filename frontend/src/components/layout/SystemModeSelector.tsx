import React from 'react';
import { Dropdown, Button } from 'antd';
import { SwapOutlined } from '@ant-design/icons';
import type { SystemMode } from '../../types/layout';
import { useTranslation } from 'react-i18next';

interface SystemModeSelectorProps {
  value: SystemMode;
  onChange: (mode: SystemMode) => void;
}

/** 模块级常量拿不到 t，改为组件内按 key 取 */
const LABEL_KEYS: Record<SystemMode, string> = {
  admin: 'mode.admin',
  app: 'mode.appSystem',
};

/**
 * SystemModeSelector 组件 - 系统模式选择器
 * 通过 Dropdown 切换"系统管理"和"应用系统"
 */
const SystemModeSelector: React.FC<SystemModeSelectorProps> = ({ value, onChange }) => {
  const { t } = useTranslation('common');
  const items = (Object.keys(LABEL_KEYS) as SystemMode[]).map((mode) => ({
    key: mode,
    label: t(LABEL_KEYS[mode]),
    disabled: mode === value,
  }));

  return (
    <Dropdown
      menu={{ items, onClick: ({ key }) => onChange(key as SystemMode) }}
      placement="bottom"
    >
      <Button type="text" icon={<SwapOutlined />}>
        {t(LABEL_KEYS[value])}
      </Button>
    </Dropdown>
  );
};

export default React.memo(SystemModeSelector);
