import { Button, Card, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import type { RedisKeyDefineVO } from '../../../services/monitorApi';
import { formatTtl } from '../../../utils/format';

interface Props {
  defines: RedisKeyDefineVO[];
  onQuickSearch: (pattern: string) => void;
}

export default function KeyDefineTable({ defines, onQuickSearch }: Props) {
  const { t } = useTranslation(['monitor', 'common']);

  const columns: ColumnsType<RedisKeyDefineVO> = [
    { title: t('common:type'), dataIndex: 'keyType', width: 150 },
    {
      title: t('keyDefine.template'),
      dataIndex: 'keyTemplate',
      render: (v: string) => <Tag color="processing">{v}</Tag>,
    },
    { title: t('keyDefine.desc'), dataIndex: 'description' },
    {
      title: 'TTL',
      dataIndex: 'timeout',
      width: 140,
      render: (v: number) => formatTtl(v, t),
    },
    {
      title: t('table.action'),
      key: 'action',
      width: 100,
      render: (_: unknown, record: RedisKeyDefineVO) => (
        <Button type="link" size="small" onClick={() => onQuickSearch(record.keyTemplate)}>
          {t('keyDefine.query')}
        </Button>
      ),
    },
  ];

  return (
    <Card title={t('keyDefine.title')} size="small">
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
