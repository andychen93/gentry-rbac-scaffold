import React from 'react';
import { Segmented } from 'antd';
import { UnorderedListOutlined, AppstoreOutlined } from '@ant-design/icons';
import type { ViewMode } from '../../types/layout';

interface ViewModeSelectorProps {
  value: ViewMode;
  onChange: (mode: ViewMode) => void;
}

const OPTIONS = [
  { value: 'list' as ViewMode, icon: <UnorderedListOutlined /> },
  { value: 'card' as ViewMode, icon: <AppstoreOutlined /> },
];

/**
 * ViewModeSelector 组件 - 视图模式选择器
 * 使用 Segmented 切换列表视图和卡片视图
 */
const ViewModeSelector: React.FC<ViewModeSelectorProps> = ({ value, onChange }) => {
  return (
    <Segmented
      value={value}
      onChange={(val) => onChange(val as ViewMode)}
      options={OPTIONS}
      size="small"
    />
  );
};

export default React.memo(ViewModeSelector);
