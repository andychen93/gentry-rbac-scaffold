import { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, Radio, Switch, message } from 'antd';
import { menuApi } from '../../services/menuApi';
import IconPicker from '../../components/common/IconPicker';

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
        message.error('菜单不存在');
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
        message.success('编辑成功');
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
        message.success('新增成功');
      }
      onSuccess();
    } catch (err: any) {
      if (err?.errorFields) return;
    } finally {
      setLoading(false);
    }
  };

  const getTitle = () => {
    if (isEdit) return '编辑菜单';
    if (parentName) return `新增子菜单 - ${parentName}`;
    return '新增菜单';
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
        <Form.Item name="type" label="菜单类型" rules={[{ required: true, message: '请选择菜单类型' }]}>
          <Radio.Group disabled={isEdit} onChange={(e) => handleTypeChange(e.target.value)}>
            <Radio.Button value={TYPE_DIR}>目录</Radio.Button>
            <Radio.Button value={TYPE_MENU}>菜单</Radio.Button>
            <Radio.Button value={TYPE_BUTTON}>按钮</Radio.Button>
          </Radio.Group>
        </Form.Item>

        {/* 菜单名称 */}
        <Form.Item
          name="name"
          label="菜单名称"
          rules={[
            { required: true, message: '请输入菜单名称' },
            { min: 2, max: 50, message: '2-50字符' },
          ]}
        >
          <Input placeholder="请输入菜单名称" />
        </Form.Item>

        {/* 图标（目录、菜单显示） */}
        {menuType !== TYPE_BUTTON && (
          <Form.Item name="icon" label="菜单图标">
            <IconPicker />
          </Form.Item>
        )}

        {/* 路由地址（目录、菜单显示） */}
        {menuType !== TYPE_BUTTON && (
          <Form.Item
            name="path"
            label="路由地址"
            rules={menuType === TYPE_MENU ? [{ required: true, message: '路由地址不能为空' }] : []}
          >
            <Input placeholder="请输入路由地址" />
          </Form.Item>
        )}

        {/* 组件路径（仅菜单显示） */}
        {menuType === TYPE_MENU && (
          <Form.Item
            name="component"
            label="组件路径"
            rules={[{ required: true, message: '组件路径不能为空' }]}
          >
            <Input placeholder="请输入组件路径" />
          </Form.Item>
        )}

        {/* 权限标识（菜单可选、按钮必填） */}
        {(menuType === TYPE_MENU || menuType === TYPE_BUTTON) && (
          <Form.Item
            name="permission"
            label="权限标识"
            rules={menuType === TYPE_BUTTON ? [{ required: true, message: '按钮权限标识不能为空' }] : []}
          >
            <Input placeholder="如 system:user:add" />
          </Form.Item>
        )}

        {/* 排序 */}
        <Form.Item name="sort" label="显示排序" rules={[{ required: true, message: '请输入排序值' }]}>
          <InputNumber min={0} max={999} style={{ width: '100%' }} />
        </Form.Item>

        {/* 状态 */}
        <Form.Item name="status" label="状态" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>

        {/* 以下仅菜单类型显示 */}
        {menuType === TYPE_MENU && (
          <>
            <Form.Item name="visible" label="是否显示" valuePropName="checked">
              <Switch checkedChildren="显示" unCheckedChildren="隐藏" />
            </Form.Item>
            <Form.Item name="isExternal" label="是否外链" valuePropName="checked">
              <Switch checkedChildren="是" unCheckedChildren="否" />
            </Form.Item>
            <Form.Item name="isCache" label="是否缓存" valuePropName="checked">
              <Switch checkedChildren="是" unCheckedChildren="否" />
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
}
