import React, { type ReactNode } from 'react';
import { Modal, Button, theme } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { argonGradients } from '@gentry/kit';
import { useTranslation } from 'react-i18next';

export type SweetAlertType = 'success' | 'error' | 'warning' | 'info';

const ICONS: Record<SweetAlertType, React.ComponentType<{ style?: React.CSSProperties }>> = {
  success: CheckCircleOutlined,
  error: CloseCircleOutlined,
  warning: WarningOutlined,
  info: InfoCircleOutlined,
};

const GRADIENT_KEY: Record<SweetAlertType, 'success' | 'danger' | 'warning' | 'info'> = {
  success: 'success',
  error: 'danger',
  warning: 'warning',
  info: 'info',
};

const BTN_CLASS: Record<SweetAlertType, string> = {
  success: 'ps-btn-gradient-success',
  error: 'ps-btn-gradient-danger',
  warning: 'ps-btn-gradient-warning',
  info: 'ps-btn-gradient-info',
};

export interface SweetAlertProps {
  open: boolean;
  type: SweetAlertType;
  title: string;
  content?: ReactNode;
  confirmText?: string;
  onConfirm: () => void;
  /** 传了则显示取消按钮 */
  cancelText?: string;
  onCancel?: () => void;
}

const SweetAlert: React.FC<SweetAlertProps> = ({
  open,
  type,
  title,
  content,
  confirmText,
  onConfirm,
  cancelText,
  onCancel,
}) => {
  const { t } = useTranslation('common');
  const Icon = ICONS[type];
  // 渐变没有对应 token，只能继续从 argonColors 取（见上方 import）；
  // 纯色部分一律走 token，不再写死 hex
  const { token: themeToken } = theme.useToken();
  return (
    <Modal
      open={open}
      onCancel={onCancel ?? onConfirm}
      footer={null}
      width={420}
      centered
      closable={false}
      maskClosable={false}
    >
      <div style={{ textAlign: 'center', padding: '12px 0' }}>
        <div
          style={{
            width: 80,
            height: 80,
            borderRadius: '50%',
            background: argonGradients[GRADIENT_KEY[type]],
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 16,
            boxShadow: '0 0 2rem 0 rgba(136,152,170,.3)',
          }}
        >
          <Icon style={{ fontSize: 40, color: themeToken.colorTextLightSolid }} />
        </div>
        <h3 style={{ color: themeToken.colorTextHeading, fontSize: 22, fontWeight: 700, margin: '0 0 8px' }}>{title}</h3>
        {content && (
          <div style={{ color: themeToken.colorText, fontSize: 14, marginBottom: 20, lineHeight: 1.6 }}>{content}</div>
        )}
        <div>
          <Button type="primary" className={BTN_CLASS[type]} onClick={onConfirm} style={{ marginRight: 8 }}>
            {confirmText ?? t('confirm')}
          </Button>
          {cancelText && onCancel && (
            <Button className="ps-btn-neutral" onClick={onCancel}>
              {cancelText}
            </Button>
          )}
        </div>
      </div>
    </Modal>
  );
};

export default SweetAlert;
export { SweetAlert };
