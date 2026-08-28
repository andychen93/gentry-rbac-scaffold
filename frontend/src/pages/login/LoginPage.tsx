import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, Tabs, Select, message, theme } from 'antd';
import { UserOutlined, LockOutlined, BankOutlined, SafetyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserStore } from '../../stores/userStore';
import { authApi, tenantApi, TenantOptionVO } from '../../services/userApi';
import { APP_NAME_KEY } from '../../config/app';

const { Title, Text } = Typography;

export default function LoginPage() {
  const { token: themeToken } = theme.useToken();
  const { t } = useTranslation('login');
  const navigate = useNavigate();
  const login = useUserStore((s) => s.login);
  const [activeTab, setActiveTab] = useState<'default' | 'tenant'>('default');
  const [loading, setLoading] = useState(false);
  /**
   * 只放后端返回的真实租户；「默认租户」那条在渲染时才拼。
   * 塞进 state 的话切换语言不会重算（options 只在挂载时 fetch 一次），标签会留在旧语言。
   */
  const [tenantOptions, setTenantOptions] = useState<{ label: string; value: string }[]>([]);
  const [captchaImg, setCaptchaImg] = useState('');
  const [captchaUuid, setCaptchaUuid] = useState('');
  const [defaultForm] = Form.useForm();
  const [tenantForm] = Form.useForm();

  // 刷新验证码
  const refreshCaptcha = useCallback(() => {
    authApi.getCaptcha()
      .then((res) => {
        // 开关关闭（sys.captcha.enabled=false）时后端返回 data:null → 无图，验证码框不渲染
        setCaptchaImg(res.data?.img ?? '');
        setCaptchaUuid(res.data?.uuid ?? '');
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
        setTenantOptions(
          (res.data || []).map((tenant: TenantOptionVO) => ({
            label: tenant.name,
            value: tenant.code,
          })),
        );
      })
      .catch(() => {
        setTenantOptions([]);
      });
  }, []);

  const tenantSelectOptions = [{ label: t('defaultTenant'), value: '' }, ...tenantOptions];

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
        message.warning(t('msg.pwdExpired'));
        navigate('/profile');
      } else {
        message.success(t('msg.success'));
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

  // 验证码输入项（两个 Tab 共用同一张图）；开关关闭时后端不发图，此处在 DOM 里整个消失
  // —— 否则框还在且必填，用户没图可看，等于登录被锁死
  const captchaItem = captchaImg ? (
    <Form.Item name="captcha" rules={[{ required: true, message: t('captcha.placeholder') }]}>
      <Input
        prefix={<SafetyOutlined />}
        placeholder={t('captcha')}
        suffix={
          <img
            src={captchaImg}
            alt={t('captcha')}
            onClick={refreshCaptcha}
            title={t('captcha.refresh')}
            style={{ height: 32, cursor: 'pointer', borderRadius: 4 }}
          />
        }
      />
    </Form.Item>
  ) : null;

  const tabItems = [
    {
      key: 'default',
      label: t('tab.default'),
      children: (
        <Form form={defaultForm} name="defaultLogin" onFinish={handleLogin} layout="vertical" size="large">
          <Form.Item name="username" rules={[{ required: true, message: t('placeholder.username', { ns: 'common' }) }]}>
            <Input prefix={<UserOutlined />} placeholder={t('username', { ns: 'common' })} />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: t('placeholder.password', { ns: 'common' }) }]}>
            <Input.Password prefix={<LockOutlined />} placeholder={t('password', { ns: 'common' })} />
          </Form.Item>
          {captchaItem}
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>{t('submit')}</Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      key: 'tenant',
      label: t('tab.tenant'),
      children: (
        <Form form={tenantForm} name="tenantLogin" onFinish={handleLogin} layout="vertical" size="large"
              initialValues={{ tenantCode: '' }}>
          <Form.Item name="tenantCode" label={t('tenant')}>
            <Select options={tenantSelectOptions} placeholder={t('tenant.placeholder')} suffixIcon={<BankOutlined />} />
          </Form.Item>
          <Form.Item name="username" rules={[{ required: true, message: t('placeholder.username', { ns: 'common' }) }]}>
            <Input prefix={<UserOutlined />} placeholder={t('username', { ns: 'common' })} />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: t('placeholder.password', { ns: 'common' }) }]}>
            <Input.Password prefix={<LockOutlined />} placeholder={t('password', { ns: 'common' })} />
          </Form.Item>
          {captchaItem}
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>{t('submit')}</Button>
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
          <Title level={3} style={{ margin: 0 }}>{t(APP_NAME_KEY, { ns: 'common' })}</Title>
          <Text type="secondary">{t('subtitle')}</Text>
        </div>

        {/* TODO [多租户演进] 开放多租户后，默认登录 Tab 可考虑隐藏或改为配置控制 */}
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as 'default' | 'tenant')}
          items={tabItems}
          centered
        />

        <div style={{ textAlign: 'center', marginTop: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>{t('testAccount')}</Text>
        </div>
      </Card>
    </div>
  );
}
