import { Card, Empty } from 'antd';
import ReactECharts from 'echarts-for-react';
import type { RedisCommandStatVO } from '../../../services/monitorApi';

interface Props {
  stats: RedisCommandStatVO[];
}

const TOP = 10;

export default function CommandStatsChart({ stats }: Props) {
  if (!stats || stats.length === 0) {
    return (
      <Card title="命令调用 TOP 10" size="small">
        <Empty description="暂无命令统计数据" style={{ padding: 40 }} />
      </Card>
    );
  }

  // 按 calls 降序截取 TOP N，其余合并为"其他"
  const sorted = [...stats].sort((a, b) => (b.calls || 0) - (a.calls || 0));
  const top = sorted.slice(0, TOP);
  const others = sorted.slice(TOP);
  const pieData = top.map((s) => ({ name: s.name, value: s.calls || 0 }));
  if (others.length > 0) {
    const sumOthers = others.reduce((acc, cur) => acc + (cur.calls || 0), 0);
    if (sumOthers > 0) pieData.push({ name: '其他', value: sumOthers });
  }

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}<br/>calls: {c}<br/>占比: {d}%' },
    legend: { type: 'scroll', orient: 'horizontal', bottom: 0 },
    series: [
      {
        name: '命令调用',
        type: 'pie',
        radius: ['35%', '65%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data: pieData,
      },
    ],
  };

  return (
    <Card title="命令调用 TOP 10" size="small">
      <ReactECharts option={option} style={{ height: 300 }} notMerge />
    </Card>
  );
}
