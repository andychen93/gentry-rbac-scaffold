import React, { useState } from 'react';
import { Input, Popover, Table, theme } from 'antd';
import { SearchOutlined, CloseCircleFilled } from '@ant-design/icons';
import { usePagedList } from '../../hooks/usePagedList';
import type { ApiResult, PageResult, PageQuery } from '../../types/api';
import type { TableProps } from 'antd';

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
  /** 远程搜索字段（作为 key 传给 service） */
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

function PageSelectInner<T>(props: PageSelectProps<T>) {
  const {
    service, columns, rowKey, labelField, value, onChange,
    placeholder = '请选择', searchField, pageSize = 5, popoverWidth = 480,
    allowClear = false, cacheKey,
  } = props;
  const { token } = theme.useToken();
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const list = usePagedList<T>({
    service,
    queryKey: ['pageSelect', cacheKey ?? rowKey],
    pageSize,
    extraParams: searchField ? { [searchField]: keyword } : {},
  });

  const handleSelect = (record: T) => {
    onChange(record);
    setOpen(false);
  };

  const displayLabel = value ? String(value[labelField] ?? '') : '';

  const content = (
    <div style={{ width: popoverWidth }}>
      {searchField && (
        <Input.Search
          placeholder="输入关键字搜索..."
          onSearch={(v) => { setKeyword(v); list.setPage(1); }}
          allowClear
          style={{ marginBottom: 12 }}
        />
      )}
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
          size: 'small', showTotal: (t) => `共 ${t} 条`,
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
      onOpenChange={setOpen}
      placement="bottomLeft"
      overlayInnerStyle={{ width: popoverWidth + 24 }}
    >
      <Input
        value={displayLabel}
        placeholder={placeholder}
        readOnly
        /*
         * 不能用 Input 自带的 allowClear：antd 对 readOnly/disabled 的输入框会
         * 给清除按钮加上 ant-input-clear-icon-hidden，永远点不到。
         * 这里自己在 suffix 里放清除图标，有值时才出现。
         */
        suffix={
          allowClear && displayLabel ? (
            <CloseCircleFilled
              className="ps-page-select-clear"
              style={{ color: token.colorTextPlaceholder, cursor: 'pointer' }}
              onClick={(e) => {
                e.stopPropagation();   // 否则会顺带把弹层打开
                onChange(null);
                setOpen(false);
              }}
            />
          ) : (
            <SearchOutlined style={{ color: token.colorTextPlaceholder }} />
          )
        }
        style={{ cursor: 'pointer' }}
        onClick={() => setOpen(true)}
      />
    </Popover>
  );
}

// 保留泛型：用 `as typeof PageSelectInner` 转换 React.memo 的返回类型
export const PageSelect = React.memo(PageSelectInner) as typeof PageSelectInner;
