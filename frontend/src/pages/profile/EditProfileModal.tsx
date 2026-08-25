import { useEffect, useState } from 'react';
import { Modal, Form, Input, Radio, Select, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { authApi } from '../../services/userApi';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

interface Props {
  open: boolean;
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * 编辑个人资料弹窗。
 * 回显走 authApi.getProfile（仅需登录，不依赖 system:user:list 权限），
 * 提交走 authApi.updateProfile。
 */
export default function EditProfileModal({ open, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['profile', 'common', 'user', 'dict']);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      authApi.getProfile()
        .then((res) => {
          const d = res.data;
          form.setFieldsValue({
            nickname: d.nickname, phone: d.phone, email: d.email,
            gender: d.gender ?? 0, postName: d.postName,
          });
        })
        .catch(() => { /* 拦截器已弹 toast */ });
    }
  }, [open, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await authApi.updateProfile(values);
      message.success(t('edit.success'));
      form.resetFields();
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={t('edit.title')} open={open} onOk={handleOk}
      onCancel={() => { form.resetFields(); onCancel(); }}
      confirmLoading={loading} destroyOnHidden>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="nickname" label={t('common:nickname')}
          rules={[{ required: true, message: t('common:placeholder.nickname') }, { min: 2, max: 20, message: t('user:form.nickname.hint') }]}>
          <Input placeholder={t('common:placeholder.nickname')} />
        </Form.Item>
        <Form.Item name="phone" label={t('common:phone')} rules={[{ pattern: /^1[3-9]\d{9}$/, message: t('common:valid.phone') }]}>
          <Input placeholder={t('user:form.phone.placeholder')} />
        </Form.Item>
        <Form.Item name="email" label={t('common:email')} rules={[{ type: 'email', message: t('common:valid.email') }]}>
          <Input placeholder={t('common:placeholder.email')} />
        </Form.Item>
        <Form.Item name="gender" label={t('common:gender')}>
          <Radio.Group options={dictOptions(t, DICT_TYPES.gender, { numeric: true })} />
        </Form.Item>
        {/* value 是 sys_user_post 的字典码，与 UserFormModal 同源 */}
        <Form.Item name="postName" label={t('user:form.post')}>
          <Select
            placeholder={t('user:form.post.placeholder')}
            allowClear
            options={dictOptions(t, DICT_TYPES.userPost)}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
