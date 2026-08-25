import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Button, Space, Spin, Tree, Radio, message, Typography,
} from 'antd';
import { useTranslation } from 'react-i18next';
import { roleApi } from '../../services/roleApi';
import { menuApi } from '../../services/menuApi';
import { deptApi } from '../../services/deptApi';
import type { RoleDetailVO } from '../../services/roleApi';
import type { MenuTreeVO } from '../../services/menuApi';
import type { DeptTreeVO } from '../../services/deptApi';
import type { Key } from 'react';
import { DICT_TYPES, dictOptions } from '../../locales/dictEnum';
import { makeNavLabel } from '../../locales/navLabel';

const { Title, Text } = Typography;

// key 用 string：id 是雪花 ID，后端 Long 序列化成字符串就是为了绕开 JS Number
// 2^53-1 的安全整数上限，这里不能再转回 number。
interface TreeNode {
  key: string;
  title: string;
  children?: TreeNode[];
}

/**
 * 递归转换菜单树（key 转字符串，见 TreeNode 注释）。
 *
 * title 走 `makeNavLabel`：菜单名是 B 类内容，后端下发 `i18nKey`、前端翻。
 * 直接用 `item.name` 会让英文界面的权限树整棵是中文。
 */
function transformMenuTree(list: MenuTreeVO[], navLabel: (n: MenuTreeVO) => string): TreeNode[] {
  return list.map((item) => ({
    key: String(item.id),
    title: navLabel(item),
    children: item.children?.length ? transformMenuTree(item.children, navLabel) : undefined,
  }));
}

/** 递归转换部门树（key 转字符串，见 TreeNode 注释） */
function transformDeptTree(list: DeptTreeVO[]): TreeNode[] {
  return list.map((item) => ({
    key: String(item.id),
    title: item.name,
    children: item.children?.length ? transformDeptTree(item.children) : undefined,
  }));
}

/** 收集树中所有叶子节点 key */
function collectAllKeys(nodes: TreeNode[]): string[] {
  const keys: string[] = [];
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
function collectLeafKeys(nodes: TreeNode[]): Set<string> {
  const leafKeys = new Set<string>();
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
  // 数据权限档位取 dict namespace（dict.sys_data_scope.*），原来这里有一份硬编码拷贝
  const { t } = useTranslation(['role', 'common', 'dict']);
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  // roleId 保持字符串：它是雪花 ID，超过 JS Number 安全整数范围（2^53-1），
  // Number(id) 会精度丢失，导致保存权限时静默作用到一个不存在（或错误）的角色上。
  const roleId = id ?? '';

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
    if (!roleId) {
      message.error(t('perm.invalidId'));
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

        const mTree = transformMenuTree(menuRes.data, makeNavLabel(t));
        setMenuTreeData(mTree);

        const dTree = transformDeptTree(deptRes.data);
        setDeptTreeData(dTree);

        // 初始化菜单勾选：只设置叶子节点，Tree 组件会自动计算半选
        // menuIds/deptIds 运行时其实是字符串（后端 Long 序列化成字符串防精度丢失），
        // 类型声明是 number[] 只是历史遗留，这里统一转 String 比较/使用
        const leafKeys = collectLeafKeys(mTree);
        const checkedLeaves = (detail.menuIds || [])
          .map(String)
          .filter((k) => leafKeys.has(k));
        setCheckedMenuKeys(checkedLeaves);

        // 初始化数据权限
        setDataScope(detail.dataScope || 1);
        setSelectedDeptKeys((detail.deptIds || []).map(String));
      })
      .catch(() => {
        message.error(t('perm.loadFailed'));
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
      message.error(t('perm.customDeptRequired'));
      return;
    }

    setSaving(true);
    try {
      // 合并 checkedKeys + halfCheckedKeys
      // 保持字符串——menuIds/deptIds 是雪花 ID，Number() 会精度丢失变成别的 ID，
      // 后端 Long 字段能正确反序列化数字型字符串
      const menuIds = [...new Set([
        ...checkedMenuKeys.map(String),
        ...halfCheckedMenuKeys.map(String),
      ])];

      const deptIds = dataScope === 5
        ? selectedDeptKeys.map(String)
        : [];

      await Promise.all([
        roleApi.assignMenus(roleId, { menuIds }),
        roleApi.updateDataScope(roleId, { dataScope, deptIds }),
      ]);

      message.success(t('perm.saveSuccess'));
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
        <Spin size="large" tip={t('common:loading')} />
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
              {t('common:back')}
            </Button>
            <Title level={5} style={{ margin: 0 }}>
              {t('perm.title', { name: roleDetail?.roleName ?? '' })}
            </Title>
          </Space>
          <Button type="primary" loading={saving} onClick={handleSave}>
            {t('common:save')}
          </Button>
        </Space>
      </Card>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* 菜单权限卡片 */}
        <Card title={t('perm.menu')} style={{ flex: 1 }}>
          <Space style={{ marginBottom: 12 }}>
            <Button size="small" onClick={handleSelectAll}>{t('common:selectAll')}</Button>
            <Button size="small" onClick={handleClearAll}>{t('common:clear')}</Button>
            <Text type="secondary">
              {t('perm.selected', { count: checkedMenuKeys.length + halfCheckedMenuKeys.length })}
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
        <Card title={t('perm.data')} style={{ flex: 1 }}>
          <div style={{ marginBottom: 16 }}>
            <Text strong style={{ display: 'block', marginBottom: 8 }}>{t('perm.dataScopeLabel')}</Text>
            <Radio.Group
              value={dataScope}
              onChange={(e) => setDataScope(e.target.value)}
              optionType="default"
            >
              <Space direction="vertical">
                {dictOptions(t, DICT_TYPES.dataScope, { numeric: true }).map((opt) => (
                  <Radio key={opt.value} value={opt.value}>{opt.label}</Radio>
                ))}
              </Space>
            </Radio.Group>
          </div>

          {dataScope === 5 && (
            <div>
              <Text strong style={{ display: 'block', marginBottom: 8 }}>
                {t('perm.customDept')}{' '}
                <Text type="secondary">{t('perm.customDeptCount', { count: selectedDeptKeys.length })}</Text>
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
