import React from 'react';
import { Space, Popconfirm, Tooltip } from 'antd';
import { usePermission } from './permission';

export interface RowActionItem {
  key: string;
  /** 操作名称。显示在 Tooltip 里，同时作为 aria-label（无障碍名 + 测试定位锚点） */
  label: string;
  /**
   * 必填：图标是唯一的视觉标识（文字已移入 Tooltip），缺图标会渲染成空白按钮。
   * 刻意设为必填而非可选，让 TS 在编译期拦住漏传的调用方。
   */
  icon: React.ReactNode;
  perm?: string;
  /** 仅控制红色样式（hover 变 danger 色），不影响是否二次确认 */
  danger?: boolean;
  /**
   * 传了就用 Popconfirm 包一层做二次确认；不传则点击直接触发 onClick。
   * 若 onClick 内部已自带 Modal.confirm，就不要传这个，否则用户要确认两次。
   */
  confirmText?: string;
  onClick: () => void;
}

/**
 * 表格行内操作：纯图标 + 悬停气泡提示。
 *
 * 形态对齐 Argon 官方表格操作列（argon-dashboard-pro-react 的 .table-action
 * 就是「纯图标 + UncontrolledTooltip」），样式见 argon.less 的 .ps-row-action。
 */
const RowActions: React.FC<{ items: RowActionItem[] }> = ({ items }) => {
  const can = usePermission();
  const visible = items.filter((i) => !i.perm || can(i.perm));

  return (
    <Space size={4}>
      {visible.map((i) => {
        const needConfirm = !!i.confirmText;
        const trigger = (
          <a
            role="button"
            aria-label={i.label}
            /* <a> 不带 href 时默认不可聚焦，必须显式加 tabIndex 才能被键盘访问 */
            tabIndex={0}
            className={`ps-row-action${i.danger ? ' is-danger' : ''}`}
            /* 需确认时点击由外层 Popconfirm 接管，这里不能再绑一次（会双触发） */
            onClick={needConfirm ? undefined : i.onClick}
            onKeyDown={(e) => {
              if (e.key !== 'Enter' && e.key !== ' ') return;
              e.preventDefault();
              /*
               * 转发成一次真实 click，而不是直接调 i.onClick：
               * 需确认的项必须先弹 Popconfirm，直接调会跳过二次确认。
               */
              (e.currentTarget as HTMLElement).click();
            }}
          >
            {i.icon}
          </a>
        );

        const withTip = (
          <Tooltip title={i.label} placement="top" mouseEnterDelay={0.2}>
            {trigger}
          </Tooltip>
        );

        return needConfirm ? (
          <Popconfirm key={i.key} title={i.confirmText} onConfirm={i.onClick}>
            {withTip}
          </Popconfirm>
        ) : (
          <React.Fragment key={i.key}>{withTip}</React.Fragment>
        );
      })}
    </Space>
  );
};
export default RowActions;
export { RowActions };
