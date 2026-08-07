import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Button, Space, Spin, Tree, Radio, message, Typography,
} from 'antd';
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { roleApi } from '../../services/roleApi';
import { menuApi } from '../../services/menuApi';
import { deptApi } from '../../services/deptApi';
import type { RoleDetailVO } from '../../services/roleApi';
import type { MenuTreeVO } from '../../services/menuApi';
import type { DeptTreeVO } from '../../services/deptApi';
import type { Key } from 'react';

const { Title, Text } = Typography;

interface TreeNode {
  key: number;
  title: string;
  children?: TreeNode[];
}

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据' },
  { value: 2, label: '本部门及子部门数据' },
  { value: 3, label: '本部门数据' },
  { value: 4, label: '仅本人数据' },
  { value: 5, label: '自定义' },
];

/** 递归转换菜单树 */
function transformMenuTree(list: MenuTreeVO[]): TreeNode[] {
  return list.map((item) => ({
    key: item.id,
    title: item.name,
    children: item.children?.length ? transformMenuTree(item.children) : undefined,
  }));
}

/** 递归转换部门树 */
function transformDeptTree(list: DeptTreeVO[]): TreeNode[] {
  return list.map((item) => ({
    key: item.id,
    title: item.name,
    children: item.children?.length ? transformDeptTree(item.children) : undefined,
  }));
}

/** 收集树中所有叶子节点 key */
function collectAllKeys(nodes: TreeNode[]): number[] {
  const keys: number[] = [];
  const walk = (list: TreeNode[]) => {
    for (const n of list) {
      keys.push(n.key);
      if (n.children?.length) walk(n.children);
    }
  };
  walk(nodes);
  return keys;
}

/** 收集所有叶子节点 key（用于 Tree 的 checkedKeys 初始化） */
function collectLeafKeys(nodes: TreeNode[]): Set<number> {
  const leafKeys = new Set<number>();
  const walk = (list: TreeNode[]) => {
    for (const n of list) {
      if (!n.children || n.children.length === 0) {
        leafKeys.add(n.key);
      } else {
        walk(n.children);
      }
    }
  };
  walk(nodes);
  return leafKeys;
}

export default function PermissionPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const roleId = Number(id);

  const [initLoading, setInitLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [roleDetail, setRoleDetail] = useState<RoleDetailVO | null>(null);
  const [menuTreeData, setMenuTreeData] = useState<TreeNode[]>([]);
  const [deptTreeData, setDeptTreeData] = useState<TreeNode[]>([]);

  // 菜单权限
  const [checkedMenuKeys, setCheckedMenuKeys] = useState<Key[]>([]);
  const [halfCheckedMenuKeys, setHalfCheckedMenuKeys] = useState<Key[]>([]);

  // 数据权限
  const [dataScope, setDataScope] = useState<number>(1);
  const [selectedDeptKeys, setSelectedDeptKeys] = useState<Key[]>([]);

  // 初始化并行加载
  useEffect(() => {
    if (!roleId || isNaN(roleId)) {
      message.error('角色ID无效');
      navigate('/system/roles');
      return;
    }

    setInitLoading(true);
    Promise.all([
      roleApi.detail(roleId),
      menuApi.tree(),
      deptApi.tree(),
    ])
      .then(([roleRes, menuRes, deptRes]) => {
        const detail = roleRes.data;
        setRoleDetail(detail);

        const mTree = transformMenuTree(menuRes.data);
        setMenuTreeData(mTree);

        const dTree = transformDeptTree(deptRes.data);
        setDeptTreeData(dTree);

        // 初始化菜单勾选：只设置叶子节点，Tree 组件会自动计算半选
        const leafKeys = collectLeafKeys(mTree);
        const checkedLeaves = (detail.menuIds || []).filter((k) => leafKeys.has(k));
        setCheckedMenuKeys(checkedLeaves);

        // 初始化数据权限
        setDataScope(detail.dataScope || 1);
        setSelectedDeptKeys(detail.deptIds || []);
      })
      .catch(() => {
        message.error('加载角色信息失败');
        navigate('/system/roles');
      })
      .finally(() => setInitLoading(false));
  }, [roleId]);

  // 菜单树勾选
  const handleMenuCheck = useCallback(
    (checked: Key[] | { checked: Key[]; halfChecked: Key[] }) => {
      if (Array.isArray(checked)) {
        setCheckedMenuKeys(checked);
        setHalfCheckedMenuKeys([]);
      } else {
        setCheckedMenuKeys(checked.checked);
        setHalfCheckedMenuKeys(checked.halfChecked);
      }
    },
    [],
  );

  // 全选菜单
  const handleSelectAll = useCallback(() => {
    const allKeys = collectAllKeys(menuTreeData);
    setCheckedMenuKeys(allKeys);
    setHalfCheckedMenuKeys([]);
  }, [menuTreeData]);

  // 清空菜单
  const handleClearAll = useCallback(() => {
    setCheckedMenuKeys([]);
    setHalfCheckedMenuKeys([]);
  }, []);

  // 部门树勾选
  const handleDeptCheck = useCallback(
    (checked: Key[] | { checked: Key[]; halfChecked: Key[] }) => {
      if (Array.isArray(checked)) {
        setSelectedDeptKeys(checked);
      } else {
        setSelectedDeptKeys(checked.checked);
      }
    },
    [],
  );

  // 保存
  const handleSave = async () => {
    // 前端校验
    if (dataScope === 5 && selectedDeptKeys.length === 0) {
      message.error('自定义数据权限必须选择部门');
      return;
    }

    setSaving(true);
    try {
      // 合并 checkedKeys + halfCheckedKeys
      const menuIds = [...new Set([
        ...checkedMenuKeys.map(Number),
        ...halfCheckedMenuKeys.map(Number),
      ])];

      const deptIds = dataScope === 5
        ? selectedDeptKeys.map(Number)
        : [];

      await Promise.all([
        roleApi.assignMenus(roleId, { menuIds }),
        roleApi.updateDataScope(roleId, { dataScope, deptIds }),
      ]);

      message.success('权限保存成功');
      navigate('/system/roles');
    } catch {
      // handled by interceptor
    } finally {
      setSaving(false);
    }
  };

  if (initLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div>
      {/* 顶部操作栏 */}
      <Card style={{ marginBottom: 16 }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space>
            <Button onClick={() => navigate('/system/roles')}>
              返回
            </Button>
            <Title level={5} style={{ margin: 0 }}>
              角色权限分配 - {roleDetail?.roleName}
            </Title>
          </Space>
          <Button type="primary" loading={saving} onClick={handleSave}>
            保存
          </Button>
        </Space>
      </Card>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* 菜单权限卡片 */}
        <Card title="菜单权限" style={{ flex: 1 }}>
          <Space style={{ marginBottom: 12 }}>
            <Button size="small" onClick={handleSelectAll}>全选</Button>
            <Button size="small" onClick={handleClearAll}>清空</Button>
            <Text type="secondary">
              已选 {checkedMenuKeys.length + halfCheckedMenuKeys.length} 项
            </Text>
          </Space>
          <Tree
            checkable
            defaultExpandAll
            treeData={menuTreeData}
            checkedKeys={checkedMenuKeys}
            onCheck={handleMenuCheck}
            style={{ maxHeight: 500, overflow: 'auto' }}
          />
        </Card>

        {/* 数据权限卡片 */}
        <Card title="数据权限" style={{ flex: 1 }}>
          <div style={{ marginBottom: 16 }}>
            <Text strong style={{ display: 'block', marginBottom: 8 }}>数据权限范围</Text>
            <Radio.Group
              value={dataScope}
              onChange={(e) => setDataScope(e.target.value)}
              optionType="default"
            >
              <Space direction="vertical">
                {DATA_SCOPE_OPTIONS.map((opt) => (
                  <Radio key={opt.value} value={opt.value}>{opt.label}</Radio>
                ))}
              </Space>
            </Radio.Group>
          </div>

          {dataScope === 5 && (
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>
                自定义部门 <Text type="secondary">（已选 {selectedDeptKeys.length} 个）</Text>
              </Text>
              <Tree
                checkable
                defaultExpandAll
                treeData={deptTreeData}
                checkedKeys={selectedDeptKeys}
                onCheck={handleDeptCheck}
                style={{ maxHeight: 400, overflow: 'auto' }}
              />
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
