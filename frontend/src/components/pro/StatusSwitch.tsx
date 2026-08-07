import React from 'react';
import { Switch } from 'antd';

export interface StatusSwitchProps {
  id: string | number;
  status: number;              // 1=启用 0=停用
  onToggle: (id: string | number, checked: boolean) => Promise<void> | void;
  disabled?: boolean;
}

const StatusSwitch: React.FC<StatusSwitchProps> = ({ status, id, onToggle, disabled }) => {
  const [loading, setLoading] = React.useState(false);
  const handleChange = async (checked: boolean) => {
    setLoading(true);
    try { await onToggle(id, checked); } finally { setLoading(false); }
  };
  return <Switch checked={status === 1} onChange={handleChange} loading={loading} disabled={disabled} size="small" />;
};
export default StatusSwitch;
export { StatusSwitch };
