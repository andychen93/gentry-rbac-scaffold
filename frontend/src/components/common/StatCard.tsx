import React from 'react';
import { useQuery } from '@tanstack/react-query';
import type { ArgonVariant } from '../../theme/argonColors';

export interface StatCardProps {
  variant: ArgonVariant;
  label: string;
  value?: React.ReactNode;
  /** 数据拉取器；传入时由 fetcher 结果决定显示值（优先级高于 value） */
  fetcher?: () => Promise<number>;
  /** 0-100 */
  progress?: number;
  hint?: React.ReactNode;
}

const formatValue = (v: React.ReactNode) =>
  typeof v === 'number' ? v.toLocaleString('en-US') : v;

const StatCard: React.FC<StatCardProps> = ({ variant, label, value, fetcher, progress, hint }) => {
  const { data: fetched } = useQuery({
    queryKey: ['statCard', label],
    queryFn: () => fetcher!(),
    enabled: !!fetcher,
    staleTime: 60 * 1000,
  });
  // 优先级：fetcher 结果 > value prop；fetcher 传入但未拉到时显示占位符
  const displayValue: React.ReactNode = fetcher ? (fetched ?? '—') : value;
  return (
    <div className={`ps-stat-card ps-stat-card--${variant}`}>
      <div className="ps-stat-card__label">{label}</div>
      <div className="ps-stat-card__value">{formatValue(displayValue)}</div>
      {progress !== undefined && (
        <div className="ps-stat-card__progress">
          <div className="ps-stat-card__progress-bar" style={{ width: `${progress}%` }} />
        </div>
      )}
      {hint && <div className="ps-stat-card__hint">{hint}</div>}
    </div>
  );
};

export default StatCard;
