import React from 'react';
import { Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { dictApi, DictDataVO } from '../../services/dictApi';
import { makeDictLabel } from '../../locales/navLabel';

interface DictTagProps {
  dictType: string;
  value: string | number | null | undefined;
}

/**
 * 字典标签：根据 dictType + value 渲染对应 dictLabel。
 * - useQuery 缓存键 ['dict', dictType]，staleTime 5min，同 dictType 多行只请求一次（TanStack Query 自动去重）。
 * - value 为 null/undefined 时渲染占位符「—」。
 * - 无匹配项时回退为原 value。
 * - 标签文案走 `makeDictLabel`：字典项是 B 类内容，后端下发 `i18nKey`、前端翻，
 *   语言包缺 key 时回退库里的 `dictLabel`。
 */
const DictTag: React.FC<DictTagProps> = ({ dictType, value }) => {
  const { t } = useTranslation('dict');
  const { data } = useQuery<DictDataVO[]>({
    queryKey: ['dict', dictType],
    queryFn: async () => {
      const res = await dictApi.listData(dictType);
      return (res.data || []).filter((d) => d.status === 1);
    },
    staleTime: 5 * 60 * 1000,
  });

  if (value === null || value === undefined) {
    return <Typography.Text type="secondary">—</Typography.Text>;
  }
  const hit = data?.find((d) => String(d.dictValue) === String(value));
  return hit ? <Tag>{makeDictLabel(t)(hit)}</Tag> : <span>{String(value)}</span>;
};

export default DictTag;
export { DictTag };
