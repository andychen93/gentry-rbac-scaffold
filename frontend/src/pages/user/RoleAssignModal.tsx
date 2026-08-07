import { useEffect, useState } from 'react';
import { Modal, Transfer, message } from 'antd';
import { userApi } from '../../services/userApi';
import { roleApi, RoleListVO } from '../../services/roleApi';

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
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [dataSource, setDataSource] = useState<TransferItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open) {
      setTargetKeys(currentRoleIds.map(String));
      roleApi.list({ pageNum: 1, pageSize: 100, status: 1 })
        .then((res) => {
          setDataSource((res.data.list || []).map((r: RoleListVO) => ({
            key: String(r.id),
            title: r.roleName,
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
      message.success('角色分配成功');
      onSuccess();
    } catch {
      // handled
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="分配角色" open={open} onOk={handleOk} onCancel={onCancel}
      confirmLoading={loading} destroyOnHidden width={600}>
      <Transfer
        dataSource={dataSource}
        targetKeys={targetKeys}
        onChange={(keys) => setTargetKeys(keys as string[])}
        render={(item) => item.title}
        titles={['可选角色', '已选角色']}
        listStyle={{ width: 240, height: 300 }}
        showSearch
        filterOption={(input, item) =>
          (item.title ?? '').toLowerCase().includes(input.toLowerCase())
        }
      />
    </Modal>
  );
}
