import React, { useEffect, useRef, useState } from 'react';
import { Input, Popover, Table, theme } from 'antd';
import { SearchOutlined, CloseCircleFilled } from '@ant-design/icons';
import { usePagedList } from '../../hooks/usePagedList';
import type { ApiResult, PageResult, PageQuery } from '../../types/api';
import type { TableProps } from 'antd';
import { useTranslation } from 'react-i18next';

/** 输入停止多久后才真正发请求（毫秒） */
const SEARCH_DEBOUNCE_MS = 300;

export interface PageSelectProps<T> {
  /** 远程分页数据服务（与 ProTable 一致） */
  service: (params: PageQuery) => Promise<ApiResult<PageResult<T>>>;
  /** 表格列定义 */
  columns: TableProps<T>['columns'];
  /** 行唯一键字段名 */
  rowKey: string;
  /** 回填显示的字段（取自选中记录） */
  labelField: keyof T;
  /** 受控值：整条记录（value=record 设计） */
  value?: T | null;
  /** 选中回调，返回整条记录；allowClear 时清空会回调 null */
  onChange: (record: T | null) => void;
  placeholder?: string;
  /** 远程搜索字段（作为 key 传给 service）。不传则输入框不参与搜索 */
  searchField?: keyof T;
  /** 弹层每页条数（紧凑，默认 5） */
  pageSize?: number;
  /** 弹层宽度（默认 480） */
  popoverWidth?: number;
  /**
   * 显示清除按钮（查询条件场景必开：选错了要能清掉，否则该条件无法取消）。
   * 清空时 onChange(null)。
   */
  allowClear?: boolean;
  /**
   * react-query 缓存键后缀。同一页面放多个 PageSelect 时必须传不同值，
   * 否则它们共用 ['pageSelect', rowKey] 前缀，rowKey 又常常都是 'id'，容易互相串数据。
   */
  cacheKey?: string;
}

/**
 * 分页下拉表格选择器（形态参考 RuoYi 的下拉选择）。
 *
 * 交互约定：
 * - **输入框本身就是搜索框**，弹层里不再放第二个搜索框；
 * - 边打字边搜（{@link SEARCH_DEBOUNCE_MS} 防抖），不需要点搜索按钮或回车；
 * - 严格选择语义：只有点中表格某一行才会产生值。未选中就关掉弹层时，
 *   输入框回滚成已选项的文本，避免把打了一半的关键字当成有效筛选值提交。
 */
function PageSelectInner<T>(props: PageSelectProps<T>) {
  const { t } = useTranslation('common');
  const {
    service, columns, rowKey, labelField, value, onChange,
    placeholder, searchField, pageSize = 5, popoverWidth = 480,
    allowClear = false, cacheKey,
  } = props;
  const { token } = theme.useToken();

  const selectedLabel = value ? String(value[labelField] ?? '') : '';

  const [open, setOpen] = useState(false);
  /** 输入框里显示的文本：可能是已选项的 label，也可能是用户正在敲的关键字 */
  const [text, setText] = useState(selectedLabel);
  /** 防抖后真正下发给后端的关键字 */
  const [keyword, setKeyword] = useState('');
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const list = usePagedList<T>({
    service,
    queryKey: ['pageSelect', cacheKey ?? rowKey],
    pageSize,
    extraParams: searchField ? { [searchField]: keyword } : {},
  });

  /**
   * 只有「用户真的在打字」才安排搜索。
   *
   * 刻意不写成 useEffect(() => ..., [text])：程序回填（同步 value、关闭回滚、
   * 选中赋值）也会改 text，得靠一个 skip 标志去屏蔽，而当新旧 text 恰好相等时
   * React 会跳过重渲染、effect 不执行，标志就一直留在"已置位"状态，
   * 把下一次真实输入也一起吞掉（实测：打开弹层后第一次输入不触发搜索）。
   */
  const scheduleSearch = (kw: string) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setKeyword(kw), SEARCH_DEBOUNCE_MS);
  };
  const cancelSearch = () => {
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
      debounceRef.current = null;
    }
  };

  useEffect(() => cancelSearch, []);

  // 外部 value 变化（含被清空、表单 reset）时同步输入框文本
  useEffect(() => {
    setText(selectedLabel);
  }, [selectedLabel]);

  // 关键字变了要回到第一页，否则会停在上一次的页码上、看起来"没结果"
  useEffect(() => {
    list.setPage(1);
    // list.setPage 是 useState setter，引用稳定
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword]);

  const handleSelect = (record: T) => {
    cancelSearch();
    onChange(record);
    setText(String(record[labelField] ?? ''));
    setOpen(false);
  };

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    cancelSearch();
    // 打开时从完整列表开始（不带上次关键字）；关闭且未选中则回滚成已选项文本，
    // 不把打了一半的关键字留在框里当成有效筛选值
    setText(selectedLabel);
    if (next) setKeyword('');
  };

  const content = (
    <div style={{ width: popoverWidth }}>
      <Table<T>
        rowKey={rowKey}
        columns={columns}
        dataSource={list.data}
        loading={list.loading}
        size="small"
        scroll={{ y: 280 }}
        onRow={(record) => ({
          onClick: () => handleSelect(record),
          style: { cursor: 'pointer' },
        })}
        pagination={{
          current: list.page, pageSize: list.pageSize, total: list.total,
          size: 'small', showTotal: (n) => t('total', { count: n }),
          onChange: (p, ps) => { list.setPage(p); list.setPageSize(ps); },
        }}
      />
    </div>
  );

  return (
    <Popover
      content={content}
      trigger="click"
      open={open}
      onOpenChange={handleOpenChange}
      placement="bottomLeft"
      overlayInnerStyle={{ width: popoverWidth + 24 }}
    >
      <Input
        value={text}
        placeholder={placeholder}
        /*
         * 输入框即搜索框：不能再设 readOnly（否则没法打字，
         * 而且 antd 会给 allowClear 的清除按钮加 hidden 类，点不到）。
         */
        onChange={(e) => {
          const next = e.target.value;
          setText(next);
          scheduleSearch(next);       // 边打边搜，无需回车或点按钮
          if (!open) setOpen(true);   // 直接打字也要把候选列表带出来
        }}
        suffix={
          allowClear && text ? (
            <CloseCircleFilled
              className="ps-page-select-clear"
              style={{ color: token.colorTextPlaceholder, cursor: 'pointer' }}
              onClick={(e) => {
                e.stopPropagation();   // 否则会顺带把弹层打开
                cancelSearch();
                onChange(null);
                setText('');
                setKeyword('');
                setOpen(false);
              }}
            />
          ) : (
            <SearchOutlined style={{ color: token.colorTextPlaceholder }} />
          )
        }
        onClick={() => setOpen(true)}
      />
    </Popover>
  );
}

// 保留泛型：用 `as typeof PageSelectInner` 转换 React.memo 的返回类型
export const PageSelect = React.memo(PageSelectInner) as typeof PageSelectInner;
