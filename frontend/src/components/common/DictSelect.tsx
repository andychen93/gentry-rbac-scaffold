import { useEffect, useState } from 'react';
import { Select } from 'antd';
import { dictApi, DictDataVO } from '../../services/dictApi';
import { useTranslation } from 'react-i18next';
import { makeDictLabel } from '../../locales/navLabel';

interface Props {
  dictType: string; value?: string; onChange?: (value: string) => void;
  placeholder?: string; disabled?: boolean; allowClear?: boolean;
}

export default function DictSelect({ dictType, value, onChange, placeholder, disabled, allowClear = true }: Props) {
  const { t } = useTranslation(['common', 'dict']);
  /*
   * state 里存**原始字典项**，不存算好的 options。
   *
   * 原来这里在 useEffect 里就把 label 拼好塞进 state 了 —— 那等于把译文冻在
   * 请求发生的那一刻，切语言时 effect 不会重跑（依赖只有 dictType），下拉会留在旧语言。
   * 渲染时才算，天然跟随语言。
   */
  const [items, setItems] = useState<DictDataVO[]>([]);
  const [loading, setLoading] = useState(false);
  const dictLabel = makeDictLabel(t);

  useEffect(() => {
    setLoading(true);
    dictApi.listData(dictType)
      .then((res) => setItems((res.data || []).filter((d: DictDataVO) => d.status === 1)))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, [dictType]);

  const options = items.map((d) => ({ label: dictLabel(d), value: d.dictValue }));

  return <Select value={value} onChange={onChange} options={options} placeholder={placeholder ?? t('placeholder.select')}
    disabled={disabled} allowClear={allowClear} loading={loading} style={{ width: '100%' }} />;
}
