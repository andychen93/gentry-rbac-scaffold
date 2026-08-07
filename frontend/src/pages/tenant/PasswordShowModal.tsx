import { Modal, Typography, Alert, Space, Button, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import type { TenantCreateResultVO } from '../../services/tenantApi';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  result: TenantCreateResultVO | null;
  onConfirm: () => void;
}

export default function PasswordShowModal({ open, result, onConfirm }: Props) {
  const handleCopy = () => {
    if (result?.adminPassword) {
      navigator.clipboard.writeText(result.adminPassword).then(() => {
        message.success('密码已复制到剪贴板');
      });
    }
  };

  return (
    <Modal
      title="租户创建成功"
      open={open}
      onCancel={onConfirm}
      destroyOnHidden
      width={480}
      footer={[
        <Button key="confirm" type="primary" onClick={onConfirm}>
          我已记录，关闭
        </Button>,
      ]}
    >
      <Alert
        type="warning"
        showIcon
        message="请妥善保管管理员密码，此密码仅展示一次，关闭后无法再次查看"
        style={{ marginBottom: 24 }}
      />
      {result && (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <Text type="secondary">租户编码：</Text>
            <Text strong>{result.tenantCode}</Text>
          </div>
          <div>
            <Text type="secondary">管理员用户名：</Text>
            <Text strong>{result.adminUsername}</Text>
          </div>
          <div>
            <Text type="secondary">管理员初始密码：</Text>
            <Space>
              <Paragraph
                copyable={{ tooltips: false, onCopy: handleCopy }}
                style={{ marginBottom: 0, display: 'inline' }}
              >
                <Text strong style={{ fontSize: 16, color: '#ff4d4f' }}>
                  {result.adminPassword}
                </Text>
              </Paragraph>
              <Button size="small" onClick={handleCopy}>
                复制
              </Button>
            </Space>
          </div>
        </Space>
      )}
    </Modal>
  );
}
