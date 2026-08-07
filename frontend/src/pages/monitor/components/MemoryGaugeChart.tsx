import { Card } from 'antd';
import ReactECharts from 'echarts-for-react';
import { formatBytes } from '../../../utils/format';

interface Props {
  usedMemory: number;
  maxMemory: number;
  percent: number;
}

export default function MemoryGaugeChart({ usedMemory, maxMemory, percent }: Props) {
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
            color: [
              [0.6, '#67C23A'],
              [0.8, '#E6A23C'],
              [1, '#F56C6C'],
            ],
          },
        },
        pointer: { length: '65%', width: 4 },
        axisTick: { length: 6, lineStyle: { color: '#ccc' } },
        splitLine: { length: 12, lineStyle: { color: '#ccc' } },
        axisLabel: { distance: 20, fontSize: 10, color: '#999' },
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
      <div style={{ textAlign: 'center', color: '#666', fontSize: 12 }}>
        {formatBytes(usedMemory)} / {maxMemory > 0 ? formatBytes(maxMemory) : '未限制'}
      </div>
    </Card>
  );
}
