import { useEffect, useState } from 'react';
import { Modal, Transfer, message } from 'antd';
import { userApi } from '../../services/userApi';
import { roleApi } from '../../services/roleApi';
import { useTranslation } from 'react-i18next';
import { makeRoleLabel } from '../../locales/navLabel';

interface Props {
  open: boolean;
  userId: number;
  currentRoleIds: number[];
  onSuccess: () => void;
  onCancel: () => void;
}

interface TransferItem {
  key: string;
  title: string;
}

export default function RoleAssignModal({ open, userId, currentRoleIds, onSuccess, onCancel }: Props) {
  const { t } = useTranslation(['user', 'common', 'role']);
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [dataSource, setDataSource] = useState<TransferItem[]>([]);
  const [loading, setLoading] = useState(false);
  const roleLabel = makeRoleLabel(t);

  useEffect(() => {
    if (open) {
      setTargetKeys(currentRoleIds.map(String));
      roleApi.options()
        .then((res) => {
          setDataSource((res.data || []).map((r) => ({
            key: String(r.id),
            title: roleLabel(r),
          })));
        })
        .catch(() => setDataSource([]));
    }
  }, [open, currentRoleIds]);

  const handleOk = async () => {
    setLoading(true);
    try {
      // roleIds 是雪花 ID，超过 JS Number 安全整数范围（2^53-1），
      // 不能 map(Number) —— 会精度丢失变成一个不存在的 ID。
      // 后端 Long 字段能正确反序列化数字型字符串，原样传字符串即可。
      await userApi.assignRoles(userId, { roleIds: targetKeys });
      message.success(t('roleAssign.success'));
      onSuccess();
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={t('roleAssign.title')} open={open} onOk={handleOk} onCancel={onCancel}
      confirmLoading={loading} destroyOnHidden width={600}>
      <Transfer
        dataSource={dataSource}
        targetKeys={targetKeys}
        onChange={(keys) => setTargetKeys(keys as string[])}
        render={(item) => item.title}
        titles={[t('form.roleTransfer'), t('form.roleTransferPicked')]}
        listStyle={{ width: 240, height: 300 }}
        showSearch
        filterOption={(input, item) =>
          (item.title ?? '').toLowerCase().includes(input.toLowerCase())
        }
      />
    </Modal>
  );
}
