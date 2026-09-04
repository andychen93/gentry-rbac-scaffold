import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message, theme } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useUserStore } from '../../stores/userStore';
import { authApi } from '../../services/userApi';
import { APP_NAME_KEY } from '../../config/app';

const { Title, Text } = Typography;

export default function LoginPage() {
  const { token: themeToken } = theme.useToken();
  const { t } = useTranslation('login');
  const navigate = useNavigate();
  const login = useUserStore((s) => s.login);
  const [loading, setLoading] = useState(false);
  const [captchaImg, setCaptchaImg] = useState('');
  const [captchaUuid, setCaptchaUuid] = useState('');
  const [form] = Form.useForm();

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

  const handleLogin = async (values: any) => {
    setLoading(true);
    try {
      await login({
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
      form.setFieldValue('captcha', '');
    } finally {
      setLoading(false);
    }
  };

  // 开关关闭时后端不发图，验证码框在 DOM 里整个消失；否则框还在且必填，登录被锁死
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

        <Form form={form} name="login" onFinish={handleLogin} layout="vertical" size="large">
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

        <div style={{ textAlign: 'center', marginTop: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>{t('testAccount')}</Text>
        </div>
      </Card>
    </div>
  );
}
