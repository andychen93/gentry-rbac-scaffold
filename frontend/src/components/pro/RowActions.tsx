import React from 'react';
import { Space, Popconfirm } from 'antd';
import { useUserStore } from '../../stores/userStore';

export interface RowActionItem {
  key: string;
  label: string;
  icon?: React.ReactNode;
  perm?: string;
  danger?: boolean;
  confirmText?: string;
  onClick: () => void;
}

const RowActions: React.FC<{ items: RowActionItem[] }> = ({ items }) => {
  const hasPermission = useUserStore((s) => s.hasPermission);
  const visible = items.filter((i) => !i.perm || hasPermission(i.perm));
  return (
    <Space size="small">
      {visible.map((i) => i.danger ? (
        <Popconfirm key={i.key} title={i.confirmText ?? '确定操作？'} onConfirm={i.onClick}>
          <a style={{ color: '#ff4d4f' }}>{i.icon}{i.label}</a>
        </Popconfirm>
      ) : (
        <a key={i.key} onClick={i.onClick}>{i.icon}{i.label}</a>
      ))}
    </Space>
  );
};
export default RowActions;
export { RowActions };
