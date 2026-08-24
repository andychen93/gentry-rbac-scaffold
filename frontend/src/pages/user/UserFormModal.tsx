import { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, Radio, Switch, Transfer, message } from 'antd';
import { userApi } from '../../services/userApi';
import { roleApi } from '../../services/roleApi';
import { useTranslation } from 'react-i18next';
import DeptTreeSelect from '../../components/common/DeptTreeSelect';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';

/**
 * 职务候选。与 sys_user_post 字典的 dict_label 一致（库里 8 条）。
 * 之所以是中文常量而不是字典码：post_name 列存的就是这个中文串，见下方 Form.Item 注释。
 */
const POST_NAMES = ['首席执行官', '总监', '经理', '主管', '总架构师', '高级工程师', '工程师', '司机'];

interface Props {
  open: boolean;
  userId: number | null;
  onSuccess: () => void;
  onCancel: () => void;
}

/** Transfer 包装组件，适配 Form 的 value/onChange 协议 */
function RoleTransfer({ value, onChange, dataSource }: {
  value?: number[];
  onChange?: (val: number[]) => void;
  dataSource: { key: string; title: string }[];
}) {
  const { t } = useTranslation('user');
  const targetKeys = (value || []).map(String);
  return (
    <Transfer
      dataSource={dataSource}
      targetKeys={targetKeys}
      onChange={(keys) => onChange?.(keys.map(Number))}
      render={(item) => item.title}
      titles={[t('form.roleTransfer'), t('form.roleTransferPicked')]}
      listStyle={{ width: 210, height: 200 }}
      showSearch
      filterOption={(input, item) => (item.title ?? '').toLowerCase().includes(input.toLowerCase())}
    />
  );
}

export default function UserFormModal({ open, userId, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['user', 'common', 'dict']);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [roleOptions, setRoleOptions] = useState<{ key: string; title: string }[]>([]);
  const isEdit = userId !== null;

  // 加载角色列表
  useEffect(() => {
    if (open) {
      roleApi.options()
        .then((res) => {
          setRoleOptions((res.data || []).map((r) => ({ key: String(r.id), title: r.roleName })));
        })
        .catch(() => setRoleOptions([]));
    }
  }, [open]);

  useEffect(() => {
    if (open && isEdit) {
      userApi.detail(userId!).then((res) => {
        const d = res.data;
        form.setFieldsValue({
          username: d.username, nickname: d.nickname, deptId: d.deptId || undefined,
          phone: d.phone, email: d.email, gender: d.gender ?? 0, postName: d.postName,
          status: d.status === 1, roleIds: d.roleIds, remark: d.remark,
        });
      });
    } else if (open) {
      form.resetFields();
      form.setFieldsValue({ gender: 0, status: true });
    }
  }, [open, userId]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const payload = { ...values, status: values.status ? 1 : 0 };
      if (isEdit) {
        const { username, password, ...updateData } = payload;
        await userApi.update(userId!, updateData);
        message.success(t('common:msg.updateSuccess'));
      } else {
        await userApi.create(payload);
        message.success(t('common:msg.createSuccess'));
      }
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={isEdit ? t('form.title.edit') : t('form.title.create')} open={open} onOk={handleOk} onCancel={onCancel}
      confirmLoading={loading} destroyOnHidden width={560}>
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        <Form.Item name="username" label={t('common:username')}
          rules={[{ required: true, message: t('common:placeholder.username') }, { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/, message: t('form.username.hint') }]}>
          <Input placeholder={t('common:placeholder.username')} disabled={isEdit} />
        </Form.Item>
        <Form.Item name="nickname" label={t('common:nickname')} rules={[{ required: true, message: t('common:placeholder.nickname') }, { min: 2, max: 20, message: t('form.nickname.hint') }]}>
          <Input placeholder={t('common:placeholder.nickname')} />
        </Form.Item>
        {!isEdit && (
          <Form.Item name="password" label={t('common:password')}
            rules={[{ required: true, message: t('common:placeholder.password') }, { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,20}$/, message: t('common:valid.password') }]}>
            <Input.Password placeholder={t('common:placeholder.password')} />
          </Form.Item>
        )}
        <Form.Item name="deptId" label={t('form.dept')}>
          <DeptTreeSelect placeholder={t('form.dept.placeholder')} showRoot={false} />
        </Form.Item>
        <Form.Item name="roleIds" label={t('common:role')}>
          <RoleTransfer dataSource={roleOptions} />
        </Form.Item>
        <Form.Item name="phone" label={t('common:phone')} rules={[{ pattern: /^1[3-9]\d{9}$/, message: t('common:valid.phone') }]}>
          <Input placeholder={t('form.phone.placeholder')} />
        </Form.Item>
        <Form.Item name="email" label={t('common:email')} rules={[{ type: 'email', message: t('common:valid.email') }]}>
          <Input placeholder={t('common:placeholder.email')} />
        </Form.Item>
        <Form.Item name="gender" label={t('common:gender')}>
          {/* 选项由字典枚举驱动，不再硬编码 —— 文案走 dict namespace 的派生 key */}
          <Radio.Group options={dictOptions(t, DICT_TYPES.gender, { numeric: true })} />
        </Form.Item>
        {/*
          * 职务的 value 是**中文 label 本身**（sys_user.post_name 存的就是「经理」这种），
          * 不是字典码。改成存 sys_user_post 的码（CEO/Manager/…）才能真正 i18n，
          * 但那要一条数据迁移 + 后端导入导出配套，属数据模型变更，本批不做。
          * 这里只补齐漂移：原来只列了 6 个，库里 sys_user_post 有 8 个。
          * 已知限制：英文界面下职务下拉仍显示中文。
          */}
        <Form.Item name="postName" label={t('form.post')}>
          <Select placeholder={t('form.post.placeholder')} allowClear
            options={POST_NAMES.map((n) => ({ value: n, label: n }))} />
        </Form.Item>
        <Form.Item name="status" label={t('common:status')} valuePropName="checked">
          <Switch checkedChildren={t('common:enable')} unCheckedChildren={t('common:disable')} />
        </Form.Item>
        <Form.Item name="remark" label={t('common:remark')} rules={[{ max: 500, message: t('common:valid.max500') }]}>
          <Input.TextArea rows={3} placeholder={t('common:placeholder.remark')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
