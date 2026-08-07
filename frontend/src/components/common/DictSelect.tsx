import { useEffect, useState } from 'react';
import { Select } from 'antd';
import { dictApi, DictDataVO } from '../../services/dictApi';

interface Props {
  dictType: string; value?: string; onChange?: (value: string) => void;
  placeholder?: string; disabled?: boolean; allowClear?: boolean;
}

export default function DictSelect({ dictType, value, onChange, placeholder = '请选择', disabled, allowClear = true }: Props) {
  const [options, setOptions] = useState<{ label: string; value: string }[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    dictApi.listData(dictType)
      .then((res) => setOptions((res.data || []).filter((d: DictDataVO) => d.status === 1)
        .map((d: DictDataVO) => ({ label: d.dictLabel, value: d.dictValue }))))
      .catch(() => setOptions([]))
      .finally(() => setLoading(false));
  }, [dictType]);

  return <Select value={value} onChange={onChange} options={options} placeholder={placeholder}
    disabled={disabled} allowClear={allowClear} loading={loading} style={{ width: '100%' }} />;
}
