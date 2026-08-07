import React from 'react';
import { Dropdown, Button } from 'antd';
import { SwapOutlined } from '@ant-design/icons';
import type { SystemMode } from '../../types/layout';

interface SystemModeSelectorProps {
  value: SystemMode;
  onChange: (mode: SystemMode) => void;
}

const LABELS: Record<SystemMode, string> = {
  admin: '系统管理',
  app: '应用系统',
};

/**
 * SystemModeSelector 组件 - 系统模式选择器
 * 通过 Dropdown 切换"系统管理"和"应用系统"
 */
const SystemModeSelector: React.FC<SystemModeSelectorProps> = ({ value, onChange }) => {
  const items = (Object.keys(LABELS) as SystemMode[]).map((mode) => ({
    key: mode,
    label: LABELS[mode],
    disabled: mode === value,
  }));

  return (
    <Dropdown
      menu={{ items, onClick: ({ key }) => onChange(key as SystemMode) }}
      placement="bottom"
    >
      <Button type="text" icon={<SwapOutlined />}>
        {LABELS[value]}
      </Button>
    </Dropdown>
  );
};

export default React.memo(SystemModeSelector);
