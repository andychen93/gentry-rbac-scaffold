import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, Tabs, Select, message } from 'antd';
import { UserOutlined, LockOutlined, BankOutlined } from '@ant-design/icons';
import { useUserStore } from '../../stores/userStore';
import { tenantApi, TenantOptionVO } from '../../services/userApi';
import { APP_NAME } from '../../config/app';

const { Title, Text } = Typography;

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useUserStore((s) => s.login);
  const [activeTab, setActiveTab] = useState<'default' | 'tenant'>('default');
  const [loading, setLoading] = useState(false);
  const [tenantOptions, setTenantOptions] = useState<{ label: string; value: string }[]>([
    { label: '默认租户', value: '' },
  ]);
  const [defaultForm] = Form.useForm();
  const [tenantForm] = Form.useForm();

  // 加载租户选项
  useEffect(() => {
    tenantApi.options()
      .then((res) => {
        const opts = (res.data || []).map((t: TenantOptionVO) => ({
          label: t.name, value: t.code,
        }));
        setTenantOptions([{ label: '默认租户', value: '' }, ...opts]);
      })
      .catch(() => {
        setTenantOptions([{ label: '默认租户', value: '' }]);
      });
  }, []);

  const handleLogin = async (values: any) => {
    setLoading(true);
    try {
      const tenantCode = activeTab === 'tenant' ? (values.tenantCode || '') : '';
      if (tenantCode) {
        await login({ tenantCode, username: values.username, password: values.password });
      } else {
        await login({ username: values.username, password: values.password });
      }
      message.success('登录成功');
      // 跳到根路径，由 App.tsx 依据动态菜单的第一条路由重定向（避免写死落地页）
      navigate('/');
    } catch {
      // 错误已在拦截器中处理
    } finally {
      setLoading(false);
    }
  };

  const tabItems = [
    {
      key: 'default',
      label: '默认登录',
      children: (
        <Form form={defaultForm} name="defaultLogin" onFinish={handleLogin} layout="vertical" size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>登录</Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      key: 'tenant',
      label: '租户登录',
      children: (
        <Form form={tenantForm} name="tenantLogin" onFinish={handleLogin} layout="vertical" size="large"
              initialValues={{ tenantCode: '' }}>
          <Form.Item name="tenantCode" label="租户">
            <Select options={tenantOptions} placeholder="请选择租户" suffixIcon={<BankOutlined />} />
          </Form.Item>
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>登录</Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f0f2f5' }}>
      <Card style={{ width: 420, borderRadius: 8 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{
            width: 64, height: 64, background: '#1677ff', borderRadius: '50%',
            margin: '0 auto 16px', display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontSize: 24, fontWeight: 'bold',
          }}>P</div>
          <Title level={3} style={{ margin: 0 }}>{APP_NAME}</Title>
          <Text type="secondary">RBAC 权限管理控制台</Text>
        </div>

        {/* TODO [多租户演进] 开放多租户后，默认登录 Tab 可考虑隐藏或改为配置控制 */}
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as 'default' | 'tenant')}
          items={tabItems}
          centered
        />

        <div style={{ textAlign: 'center', marginTop: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>测试账号: admin / Abc@123456</Text>
        </div>
      </Card>
    </div>
  );
}
