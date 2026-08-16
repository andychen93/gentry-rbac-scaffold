import { Card, Typography, theme } from 'antd';
import ReactECharts from 'echarts-for-react';
import { formatBytes } from '../../../utils/format';

interface Props {
  usedMemory: number;
  maxMemory: number;
  percent: number;
}

export default function MemoryGaugeChart({ usedMemory, maxMemory, percent }: Props) {
  // ECharts 的 option 是普通对象，拿不到 CSS 变量也吃不到 antd 样式，
  // 只能把 token 值显式传进去
  const { token } = theme.useToken();

  const option = {
    series: [
      {
        type: 'gauge',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        progress: { show: true, width: 18 },
        axisLine: {
          lineStyle: {
            width: 18,
            // 内存使用率分档：≤60% 健康、≤80% 需关注、其余告警。
            // 原值 #67C23A/#E6A23C/#F56C6C 是 Element Plus 的色板，整套跑错了
            color: [
              [0.6, token.colorSuccess],
              [0.8, token.colorWarning],
              [1, token.colorError],
            ],
          },
        },
        pointer: { length: '65%', width: 4 },
        axisTick: { length: 6, lineStyle: { color: token.colorBorder } },
        splitLine: { length: 12, lineStyle: { color: token.colorBorder } },
        axisLabel: { distance: 20, fontSize: 10, color: token.colorTextTertiary },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: 'inherit',
          fontSize: 20,
          offsetCenter: [0, '70%'],
        },
        data: [{ value: Number(percent) || 0, name: '使用率' }],
      },
    ],
  };

  return (
    <Card title="内存使用率" size="small">
      <ReactECharts option={option} style={{ height: 260 }} notMerge />
      <div style={{ textAlign: 'center' }}>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {formatBytes(usedMemory)} / {maxMemory > 0 ? formatBytes(maxMemory) : '未限制'}
        </Typography.Text>
      </div>
    </Card>
  );
}
