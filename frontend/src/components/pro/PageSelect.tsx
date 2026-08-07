import React, { useState } from 'react';
import { Input, Popover, Table } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
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
  /** 选中回调，返回整条记录 */
  onChange: (record: T) => void;
  placeholder?: string;
  /** 远程搜索字段（作为 key 传给 service） */
  searchField?: keyof T;
  /** 弹层每页条数（紧凑，默认 5） */
  pageSize?: number;
  /** 弹层宽度（默认 480） */
  popoverWidth?: number;
}

function PageSelectInner<T>(props: PageSelectProps<T>) {
  const {
    service, columns, rowKey, labelField, value, onChange,
    placeholder = '请选择', searchField, pageSize = 5, popoverWidth = 480,
  } = props;
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const list = usePagedList<T>({
    service,
    queryKey: ['pageSelect', rowKey],
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
        suffix={<SearchOutlined style={{ color: '#8898aa' }} />}
        style={{ cursor: 'pointer' }}
        onClick={() => setOpen(true)}
      />
    </Popover>
  );
}

// 保留泛型：用 `as typeof PageSelectInner` 转换 React.memo 的返回类型
export const PageSelect = React.memo(PageSelectInner) as typeof PageSelectInner;
