import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, Tabs, Select, message, theme } from 'antd';
import { UserOutlined, LockOutlined, BankOutlined, SafetyOutlined } from '@ant-design/icons';
import { useUserStore } from '../../stores/userStore';
import { authApi, tenantApi, TenantOptionVO } from '../../services/userApi';
import { APP_NAME } from '../../config/app';

const { Title, Text } = Typography;

export default function LoginPage() {
  const { token: themeToken } = theme.useToken();
  const navigate = useNavigate();
  const login = useUserStore((s) => s.login);
  const [activeTab, setActiveTab] = useState<'default' | 'tenant'>('default');
  const [loading, setLoading] = useState(false);
  const [tenantOptions, setTenantOptions] = useState<{ label: string; value: string }[]>([
    { label: '默认租户', value: '' },
  ]);
  const [captchaImg, setCaptchaImg] = useState('');
  const [captchaUuid, setCaptchaUuid] = useState('');
  const [defaultForm] = Form.useForm();
  const [tenantForm] = Form.useForm();

  // 刷新验证码
  const refreshCaptcha = useCallback(() => {
    authApi.getCaptcha()
      .then((res) => {
        setCaptchaImg(res.data.img);
        setCaptchaUuid(res.data.uuid);
      })
      .catch(() => {
        // 验证码不可用时不阻塞登录（后端可能关闭了验证码开关）
      });
  }, []);

  useEffect(() => {
    refreshCaptcha();
  }, [refreshCaptcha]);

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
      await login({
        tenantCode: tenantCode || undefined,
        username: values.username,
        password: values.password,
        uuid: captchaUuid,
        captcha: values.captcha,
      });
      // 密码过期引导：不阻断登录，跳个人中心提示修改
      if (useUserStore.getState().passwordExpired) {
        message.warning('密码已过期，建议尽快修改');
        navigate('/profile');
      } else {
        message.success('登录成功');
        // 跳到根路径，由 App.tsx 依据动态菜单的第一条路由重定向（避免写死落地页）
        navigate('/');
      }
    } catch {
      // 错误已在拦截器中处理；验证码一次性，失败后刷新并清空输入
      refreshCaptcha();
      defaultForm.setFieldValue('captcha', '');
      tenantForm.setFieldValue('captcha', '');
    } finally {
      setLoading(false);
    }
  };

  // 验证码输入项（两个 Tab 共用同一张图）
  const captchaItem = (
    <Form.Item name="captcha" rules={[{ required: true, message: '请输入验证码' }]}>
      <Input
        prefix={<SafetyOutlined />}
        placeholder="验证码"
        suffix={
          captchaImg ? (
            <img
              src={captchaImg}
              alt="验证码"
              onClick={refreshCaptcha}
              title="点击刷新"
              style={{ height: 32, cursor: 'pointer', borderRadius: 4 }}
            />
          ) : null
        }
      />
    </Form.Item>
  );

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
          {captchaItem}
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
          {captchaItem}
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>登录</Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: themeToken.colorBgLayout }}>
      <Card style={{ width: 420, borderRadius: 8 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          {/* 品牌圆标用主色 token；原先的 #1677ff / #f0f2f5 是 antd 默认蓝和默认灰，
              让登录页跟站内 Argon 配色对不上 */}
          <div style={{
            width: 64, height: 64, background: themeToken.colorPrimary, borderRadius: '50%',
            margin: '0 auto 16px', display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: themeToken.colorTextLightSolid, fontSize: 24, fontWeight: 'bold',
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
