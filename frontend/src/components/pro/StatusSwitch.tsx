import React from 'react';
import { Modal, Switch } from 'antd';
import { useTranslation } from 'react-i18next';

export interface StatusSwitchProps {
  id: string | number;
  status: number;              // 1=启用 0=停用
  onToggle: (id: string | number, checked: boolean) => Promise<void> | void;
  disabled?: boolean;
  /**
   * 传了就在切换前弹二次确认，不传则点击直接生效（保持原行为）。
   * 入参是「即将切换到的状态」，方便启用/停用给出不同文案。
   */
  confirmText?: (nextEnabled: boolean) => string;
}

const StatusSwitch: React.FC<StatusSwitchProps> = ({ status, id, onToggle, disabled, confirmText }) => {
  const { t } = useTranslation('common');
  const [loading, setLoading] = React.useState(false);

  const run = async (checked: boolean) => {
    setLoading(true);
    try { await onToggle(id, checked); } finally { setLoading(false); }
  };

  const handleChange = async (checked: boolean) => {
    if (!confirmText) {
      await run(checked);
      return;
    }
    /*
     * Switch 是受控的（checked 由 status 决定），所以用户点「取消」时开关不会自己动，
     * 不需要手动回滚 UI。onOk 返回 Promise 让弹窗按钮显示 loading 并等请求结束再关。
     */
    Modal.confirm({
      title: checked ? t('common:confirm.enable') : t('common:confirm.disable'),
      content: confirmText(checked),
      okText: t('common:confirm'),
      cancelText: t('common:cancel'),
      okButtonProps: { danger: !checked },
      onOk: () => run(checked),
    });
  };

  return <Switch checked={status === 1} onChange={handleChange} loading={loading} disabled={disabled} size="small" />;
};
export default StatusSwitch;
export { StatusSwitch };
