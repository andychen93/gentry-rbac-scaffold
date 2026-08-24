import { useEffect, useState, useMemo } from 'react';
import { TreeSelect } from 'antd';
import { deptApi } from '../../services/deptApi';
import type { DeptTreeVO } from '../../services/deptApi';
import { useTranslation } from 'react-i18next';

interface TreeNode {
  title: string;
  value: number;
  children?: TreeNode[];
}

interface Props {
  value?: number | null;
  onChange?: (value: number | null) => void;
  placeholder?: string;
  allowClear?: boolean;
  disabled?: boolean;
  showRoot?: boolean;
  excludeId?: number | null;
}

/**
 * 部门树选择器（公共组件，供其他模块复用）
 */
export default function DeptTreeSelect({
  value,
  onChange,
  placeholder,
  allowClear = true,
  disabled = false,
  showRoot = true,
  excludeId,
}: Props) {
  const { t } = useTranslation('common');
  const [treeData, setTreeData] = useState<DeptTreeVO[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    deptApi.tree()
      .then((res) => setTreeData(res.data ?? []))
      .catch(() => setTreeData([]))
      .finally(() => setLoading(false));
  }, []);

  const treeSelectData = useMemo(() => {
    const buildNodes = (list: DeptTreeVO[]): TreeNode[] =>
      list
        .filter((item) => item.id !== excludeId)
        .map((item) => ({
          title: item.name,
          value: item.id,
          children: item.children?.length ? buildNodes(item.children) : undefined,
        }));

    const nodes = buildNodes(treeData);
    if (showRoot) {
      return [{ title: t('common:dept.noParent'), value: 0, children: nodes }];
    }
    return nodes;
  }, [treeData, showRoot, excludeId]);

  return (
    <TreeSelect
      value={value}
      onChange={(val) => onChange?.(val ?? null)}
      treeData={treeSelectData}
      placeholder={placeholder ?? t('common:placeholder.selectParentDept')}
      allowClear={allowClear}
      disabled={disabled}
      loading={loading}
      treeDefaultExpandAll
      style={{ width: '100%' }}
    />
  );
}
