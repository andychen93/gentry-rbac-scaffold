import React from 'react';
import { PageSelect } from '@gentry/kit';
import { menuApi } from '../../services/menuApi';
import type { MenuTreeVO } from '../../services/menuApi';
import type { ApiResult, PageQuery, PageResult } from '@gentry/kit';
import { useTranslation } from 'react-i18next';
import { DICT_TYPES, dictLabel } from '../../locales/dictEnum';

/** 拍平后的菜单行（只保留下拉表格要显示的字段 + 层级路径） */
export interface MenuFlatVO {
  id: number;
  name: string;
  type: number;
  /** 「系统管理 / 用户管理」这样的祖先路径，用于区分同名菜单 */
  fullPath: string;
}

/**
 * 把菜单树拍平成列表，只保留目录与菜单（type 1/2），丢掉按钮。
 *
 * 丢按钮的原因：按钮名都是「查询/新增/编辑/删除」，作为「模块」候选毫无意义，
 * 而且数量占了菜单表的大半，会把有用的选项挤出可视区。
 */
function flatten(nodes: MenuTreeVO[], parentPath = ''): MenuFlatVO[] {
  const out: MenuFlatVO[] = [];
  for (const n of nodes) {
    const fullPath = parentPath ? `${parentPath} / ${n.name}` : n.name;
    if (n.type === 1 || n.type === 2) {
      out.push({ id: n.id, name: n.name, type: n.type, fullPath });
    }
    if (n.children?.length) out.push(...flatten(n.children, fullPath));
  }
  return out;
}

/**
 * 菜单树没有分页接口，这里在前端拍平 + 模糊过滤 + 切片，凑成 PageResult。
 * 菜单是全局表且量很小（几十条），一次性拉回来完全够用；
 * 同样的适配手法见 pages/log/OnlineUserPage 的 listOnlineUsersPaged。
 */
async function listMenusPaged(params: PageQuery): Promise<ApiResult<PageResult<MenuFlatVO>>> {
  const { pageNum = 1, pageSize = 5, name } = params;
  const res = await menuApi.tree();
  const all = flatten(res.data || []);
  const kw = String(name ?? '').trim().toLowerCase();
  const filtered = kw ? all.filter((m) => m.fullPath.toLowerCase().includes(kw)) : all;
  const start = (Number(pageNum) - 1) * Number(pageSize);
  return {
    code: 0,
    data: {
      list: filtered.slice(start, start + Number(pageSize)),
      total: filtered.length,
      pageNum: Number(pageNum),
      pageSize: Number(pageSize),
      pages: Math.ceil(filtered.length / Number(pageSize)),
    },
  };
}

interface Props {
  /** 受控值：菜单名称字符串（日志表 module 存的是模块中文名） */
  value?: string;
  onChange?: (value?: string) => void;
  placeholder?: string;
}

/**
 * 菜单「下拉 table」选择器，用于操作日志按「模块」筛选。
 *
 * 值语义是菜单 name 而非 id：sys_oper_log.module 存的是 @Log(module="xxx")
 * 注解里写的中文模块名，后端按 `LIKE '%module%'` 过滤。
 * 注意二者是「约定对齐」而不是外键，个别模块名（如「日志管理」）在菜单表里
 * 没有完全同名的行，反之菜单里也有从未产生日志的项 —— 选中后查不到结果是正常的。
 */
const MenuTableSelect: React.FC<Props> = ({ value, onChange, placeholder }) => {
  const { t } = useTranslation(['common', 'dict']);
  const pseudoRecord = value ? ({ name: value } as MenuFlatVO) : null;

  return (
    <PageSelect<MenuFlatVO>
      cacheKey="menuTableSelect"
      service={listMenusPaged}
      columns={[
        /*
         * name 列**故意不翻译**：labelField 也是 name，选中后提交的就是这一列的值，
         * 而 sys_oper_log.module 存的是 @Log(module="…") 注解里的中文字面量。
         * 显示英文、提交英文，筛选就永远查不到结果。@Log 的 i18n 见下个版本计划。
         */
        { title: t('name'), dataIndex: 'name', width: 130 },
        {
          // 参数原名叫 t，会遮蔽翻译函数 t —— 改名 type
          title: t('type'), dataIndex: 'type', width: 70,
          render: (type: number) => dictLabel(t, DICT_TYPES.menuType, type),
        },
        { title: t('position'), dataIndex: 'fullPath', ellipsis: true },
      ]}
      rowKey="id"
      labelField="name"
      searchField="name"
      value={pseudoRecord}
      onChange={(record) => onChange?.(record ? record.name : undefined)}
      // 兜底值放函数体、不放默认参数：默认参数在组件外求值，那里没有 t
      placeholder={placeholder ?? t('placeholder.searchMenu')}
      allowClear
      popoverWidth={460}
    />
  );
};

export default MenuTableSelect;
export { MenuTableSelect };
