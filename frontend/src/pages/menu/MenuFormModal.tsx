import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Radio, Switch, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { menuApi } from '../../services/menuApi';
import IconPicker from '../../components/common/IconPicker';
import { DICT_TYPES, dictLabel } from '../../locales/dictEnum';

interface Props {
  open: boolean;
  menuId: number | null;
  parentId: number | null;
  parentName: string | null;
  onSuccess: () => void;
  onCancel: () => void;
}

/** 菜单类型 */
const TYPE_DIR = 1;
const TYPE_MENU = 2;
const TYPE_BUTTON = 3;

export default function MenuFormModal({ open, menuId, parentId, parentName, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['menuMgmt', 'common', 'dict']);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [menuType, setMenuType] = useState<number>(TYPE_DIR);
  const isEdit = menuId !== null;

  useEffect(() => {
    if (open && isEdit) {
      menuApi.detail(menuId!).then((res) => {
        const d = res.data;
        setMenuType(d.type);
        form.setFieldsValue({
          type: d.type,
          name: d.name,
          icon: d.icon,
          path: d.path,
          component: d.component,
          permission: d.permission,
          sort: d.sort,
          visible: d.visible === 1,
          status: d.status === 1,
          isExternal: d.isExternal === 1,
          isCache: d.isCache === 1,
        });
      }).catch(() => {
        message.error(t('msg.notFound'));
        onCancel();
      });
    } else if (open) {
      form.resetFields();
      setMenuType(TYPE_DIR);
      form.setFieldsValue({ type: TYPE_DIR, sort: 0, visible: true, status: true, isExternal: false, isCache: false });
    }
  }, [open, menuId, form, isEdit, onCancel]);

  const handleTypeChange = (val: number) => {
    setMenuType(val);
    // 清除不适用的字段
    if (val === TYPE_DIR) {
      form.setFieldsValue({ component: undefined, permission: undefined, visible: true, isExternal: false, isCache: false });
    } else if (val === TYPE_BUTTON) {
      form.setFieldsValue({ icon: null, path: undefined, component: undefined, visible: true, isExternal: false, isCache: false });
    }
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit) {
        await menuApi.update(menuId!, {
          parentId: parentId ?? undefined,
          name: values.name,
          icon: values.icon || undefined,
          sort: values.sort,
          permission: values.permission || undefined,
          path: values.path || undefined,
          component: values.component || undefined,
          visible: values.visible ? 1 : 0,
          status: values.status ? 1 : 0,
          isExternal: values.isExternal ? 1 : 0,
          isCache: values.isCache ? 1 : 0,
        });
        message.success(t('common:msg.updateSuccess'));
      } else {
        await menuApi.create({
          parentId: parentId ?? 0,
          name: values.name,
          icon: values.icon || undefined,
          type: values.type,
          sort: values.sort,
          permission: values.permission || undefined,
          path: values.path || undefined,
          component: values.component || undefined,
          visible: values.visible ? 1 : 0,
          status: values.status ? 1 : 0,
          isExternal: values.isExternal ? 1 : 0,
          isCache: values.isCache ? 1 : 0,
        });
        message.success(t('common:msg.createSuccess'));
      }
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  const getTitle = () => {
    if (isEdit) return t('form.title.edit');
    if (parentName) return t('form.title.createChild', { parent: parentName });
    return t('form.title.create');
  };

  return (
    <Modal
      title={getTitle()}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={600}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        {/* 菜单类型 */}
        <Form.Item name="type" label={t('form.type')} rules={[{ required: true, message: t('form.type.required') }]}>
          {/* 三个档位的文案取 sys_menu_type 字典，不再各页硬编码 */}
          <Radio.Group disabled={isEdit} onChange={(e) => handleTypeChange(e.target.value)}>
            <Radio.Button value={TYPE_DIR}>{dictLabel(t, DICT_TYPES.menuType, TYPE_DIR)}</Radio.Button>
            <Radio.Button value={TYPE_MENU}>{dictLabel(t, DICT_TYPES.menuType, TYPE_MENU)}</Radio.Button>
            <Radio.Button value={TYPE_BUTTON}>{dictLabel(t, DICT_TYPES.menuType, TYPE_BUTTON)}</Radio.Button>
          </Radio.Group>
        </Form.Item>

        {/* 菜单名称 */}
        <Form.Item
          name="name"
          label={t('form.name')}
          rules={[
            { required: true, message: t('form.name.placeholder') },
            { min: 2, max: 50, message: t('common:valid.len2to50') },
          ]}
        >
          <Input placeholder={t('form.name.placeholder')} />
        </Form.Item>

        {/* 图标（目录、菜单显示） */}
        {menuType !== TYPE_BUTTON && (
          <Form.Item name="icon" label={t('form.icon')}>
            <IconPicker />
          </Form.Item>
        )}

        {/* 路由地址（目录、菜单显示） */}
        {menuType !== TYPE_BUTTON && (
          <Form.Item
            name="path"
            label={t('form.path')}
            rules={menuType === TYPE_MENU ? [{ required: true, message: t('form.path.required') }] : []}
          >
            <Input placeholder={t('form.path.placeholder')} />
          </Form.Item>
        )}

        {/* 组件路径（仅菜单显示） */}
        {menuType === TYPE_MENU && (
          <Form.Item
            name="component"
            label={t('form.component')}
            rules={[{ required: true, message: t('form.component.required') }]}
          >
            <Input placeholder={t('form.component.placeholder')} />
          </Form.Item>
        )}

        {/* 权限标识（菜单可选、按钮必填） */}
        {(menuType === TYPE_MENU || menuType === TYPE_BUTTON) && (
          <Form.Item
            name="permission"
            label={t('form.permission')}
            rules={menuType === TYPE_BUTTON ? [{ required: true, message: t('form.permission.required') }] : []}
          >
            <Input placeholder={t('form.permission.placeholder')} />
          </Form.Item>
        )}

        {/* 排序 */}
        <Form.Item name="sort" label={t('form.sort')} rules={[{ required: true, message: t('form.sort.required') }]}>
          <InputNumber min={0} max={999} style={{ width: '100%' }} />
        </Form.Item>

        {/* 状态 */}
        <Form.Item name="status" label={t('common:status')} valuePropName="checked">
          <Switch checkedChildren={t('common:enable')} unCheckedChildren={t('common:disable')} />
        </Form.Item>

        {/* 以下仅菜单类型显示 */}
        {menuType === TYPE_MENU && (
          <>
            <Form.Item name="visible" label={t('form.visible')} valuePropName="checked">
              <Switch
                checkedChildren={t('common:visible.show')}
                unCheckedChildren={t('common:visible.hide')}
              />
            </Form.Item>
            <Form.Item name="isExternal" label={t('form.isExternal')} valuePropName="checked">
              <Switch checkedChildren={t('common:yes')} unCheckedChildren={t('common:no')} />
            </Form.Item>
            <Form.Item name="isCache" label={t('form.isCache')} valuePropName="checked">
              <Switch checkedChildren={t('common:yes')} unCheckedChildren={t('common:no')} />
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
}
