import { useEffect, useState } from 'react';
import { Select } from 'antd';
import { dictApi, DictDataVO } from '../../services/dictApi';
import { useTranslation } from 'react-i18next';

interface Props {
  dictType: string; value?: string; onChange?: (value: string) => void;
  placeholder?: string; disabled?: boolean; allowClear?: boolean;
}

export default function DictSelect({ dictType, value, onChange, placeholder, disabled, allowClear = true }: Props) {
  const { t } = useTranslation('common');
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

  return <Select value={value} onChange={onChange} options={options} placeholder={placeholder ?? t('placeholder.select')}
    disabled={disabled} allowClear={allowClear} loading={loading} style={{ width: '100%' }} />;
}
