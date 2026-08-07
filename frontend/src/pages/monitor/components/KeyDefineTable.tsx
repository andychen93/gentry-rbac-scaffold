import { Button, Card, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { RedisKeyDefineVO } from '../../../services/monitorApi';
import { formatTtl } from '../../../utils/format';

interface Props {
  defines: RedisKeyDefineVO[];
  onQuickSearch: (pattern: string) => void;
}

export default function KeyDefineTable({ defines, onQuickSearch }: Props) {
  const columns: ColumnsType<RedisKeyDefineVO> = [
    { title: '类型', dataIndex: 'keyType', width: 150 },
    {
      title: 'Key 模板',
      dataIndex: 'keyTemplate',
      render: (v: string) => <Tag color="processing">{v}</Tag>,
    },
    { title: '说明', dataIndex: 'description' },
    {
      title: 'TTL',
      dataIndex: 'timeout',
      width: 140,
      render: (v: number) => formatTtl(v),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: RedisKeyDefineVO) => (
        <Button type="link" size="small" onClick={() => onQuickSearch(record.keyTemplate)}>
          查询 Key
        </Button>
      ),
    },
  ];

  return (
    <Card title="Key 定义" size="small">
      <Table<RedisKeyDefineVO>
        rowKey="keyType"
        columns={columns}
        dataSource={defines}
        pagination={false}
        size="small"
      />
    </Card>
  );
}
