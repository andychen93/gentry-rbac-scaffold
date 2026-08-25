import { useEffect, useState } from 'react';
import { Modal, Transfer, message } from 'antd';
import { useTranslation } from 'react-i18next';
import { roleApi } from '../../services/roleApi';
import { userApi } from '../../services/userApi';

interface Props {
  open: boolean;
  roleId: number | string;
  roleName?: string;
  onSuccess: () => void;
  onCancel: () => void;
}

interface TransferItem {
  key: string;
  title: string;
  description: string;
}

/**
 * 角色 → 绑定用户（穿梭框，全量覆盖保存）。
 *
 * 与 pages/user/RoleAssignModal 是对称的两个方向：那边是「一个用户挂哪些角色」，
 * 这边是「一个角色挂哪些用户」，都写 sys_user_role。
 */
export default function UserAssignModal({ open, roleId, roleName, onSuccess, onCancel }: Props) {
  const { t } = useTranslation('role');
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [dataSource, setDataSource] = useState<TransferItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(false);

  useEffect(() => {
    if (!open) return;
    setFetching(true);
    // 候选用户与已绑定 ID 一起取，避免先渲染空列表再跳变
    Promise.all([userApi.options(), roleApi.listUserIds(roleId)])
      .then(([optRes, assignedRes]) => {
        const options = optRes.data || [];
        setDataSource(
          options.map((u) => ({
            key: String(u.id),
            title: u.nickname ? `${u.nickname}（${u.username}）` : u.username,
            description: u.deptName ?? '',
          })),
        );
        /*
         * 只保留仍在候选列表里的已绑定 ID。
         *
         * 候选只含「本租户 + 启用 + 未删除」的用户，而已绑定 ID 直读关联表，
         * 可能含已停用/已删除的用户。这类 ID 在穿梭框里根本渲染不出来，
         * 若原样留在 targetKeys 里，保存时会把「界面上看不见的东西」一起提交
         * —— 用户管理页的「角色不存在」就是这个坑造成的，这里不重犯。
         */
        const selectable = new Set(options.map((u) => String(u.id)));
        setTargetKeys((assignedRes.data || []).map(String).filter((id) => selectable.has(id)));
      })
      .catch(() => {
        setDataSource([]);
        setTargetKeys([]);
      })
      .finally(() => setFetching(false));
  }, [open, roleId]);

  const handleOk = async () => {
    setLoading(true);
    try {
      // 雪花 ID 原样传字符串，不能 map(Number)（会精度丢失变成不存在的 ID）
      await roleApi.assignUsers(roleId, { userIds: targetKeys });
      message.success(t('assign.success'));
      onSuccess();
    } catch {
      /* 错误提示由 request 拦截器统一弹 */
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={roleName ? t('assign.titleWith', { name: roleName }) : t('assign.title')}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      destroyOnHidden
      width={660}
    >
      <Transfer
        dataSource={dataSource}
        targetKeys={targetKeys}
        onChange={(keys) => setTargetKeys(keys as string[])}
        render={(item) => item.title}
        titles={[t('assign.available'), t('assign.picked')]}
        listStyle={{ width: 280, height: 320 }}
        showSearch
        disabled={fetching}
        filterOption={(input, item) => {
          const kw = input.toLowerCase();
          return (
            (item.title ?? '').toLowerCase().includes(kw) ||
            (item.description ?? '').toLowerCase().includes(kw)
          );
        }}
      />
    </Modal>
  );
}
