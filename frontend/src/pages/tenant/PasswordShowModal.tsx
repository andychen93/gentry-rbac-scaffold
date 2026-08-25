import { Modal, Typography, Alert, Space, Button, message } from 'antd';
import { useTranslation } from 'react-i18next';
import type { TenantCreateResultVO } from '../../services/tenantApi';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  result: TenantCreateResultVO | null;
  onConfirm: () => void;
}

export default function PasswordShowModal({ open, result, onConfirm }: Props) {
  const { t } = useTranslation(['tenant', 'common']);

  const handleCopy = () => {
    if (result?.adminPassword) {
      navigator.clipboard.writeText(result.adminPassword).then(() => {
        message.success(t('created.copied'));
      });
    }
  };

  return (
    <Modal
      title={t('created.title')}
      open={open}
      onCancel={onConfirm}
      destroyOnHidden
      width={480}
      footer={[
        <Button key="confirm" type="primary" onClick={onConfirm}>
          {t('created.close')}
        </Button>,
      ]}
    >
      <Alert
        type="warning"
        showIcon
        message={t('created.warning')}
        style={{ marginBottom: 24 }}
      />
      {result && (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <Text type="secondary">{t('created.tenantCode')}</Text>
            <Text strong>{result.tenantCode}</Text>
          </div>
          <div>
            <Text type="secondary">{t('created.adminUsername')}</Text>
            <Text strong>{result.adminUsername}</Text>
          </div>
          <div>
            <Text type="secondary">{t('created.adminPassword')}</Text>
            <Space>
              <Paragraph
                copyable={{ tooltips: false, onCopy: handleCopy }}
                style={{ marginBottom: 0, display: 'inline' }}
              >
                {/* type="danger" 走 antd colorError token（argonTheme 里指向 argonColors.danger），
                    不写死颜色 —— 原本的 #ff4d4f 是 antd 默认红，和 Argon 的 #f5365c 不是一个色 */}
                <Text strong type="danger" style={{ fontSize: 16 }}>
                  {result.adminPassword}
                </Text>
              </Paragraph>
              <Button size="small" onClick={handleCopy}>
                {t('common:copy')}
              </Button>
            </Space>
          </div>
        </Space>
      )}
    </Modal>
  );
}
