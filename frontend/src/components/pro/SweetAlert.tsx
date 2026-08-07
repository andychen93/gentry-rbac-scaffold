import React, { type ReactNode } from 'react';
import { Modal, Button } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import { argonGradients } from '../../theme/argonColors';

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
  confirmText = '确认',
  onConfirm,
  cancelText,
  onCancel,
}) => {
  const Icon = ICONS[type];
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
          <Icon style={{ fontSize: 40, color: '#fff' }} />
        </div>
        <h3 style={{ color: '#32325d', fontSize: 22, fontWeight: 700, margin: '0 0 8px' }}>{title}</h3>
        {content && (
          <div style={{ color: '#525f7f', fontSize: 14, marginBottom: 20, lineHeight: 1.6 }}>{content}</div>
        )}
        <div>
          <Button type="primary" className={BTN_CLASS[type]} onClick={onConfirm} style={{ marginRight: 8 }}>
            {confirmText}
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
