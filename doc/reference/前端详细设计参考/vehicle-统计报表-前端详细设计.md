# 前端详细设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | 统计报表 前端详细设计文档 |
| 对应后端模块 | 统计报表（模块标识：stats） |
| 参考文档 | `doc/design/modules/vehicle/modules/统计报表/后端详细设计.md` |
| 静态页面 | 暂无（待创建） |
| 版本 | v1.0.0 |
| 创建日期 | 2026-04-12 |
| 负责人 | 前端开发（AI辅助） |

---

# 一、模块概述

本模块前端实现**统计报表**功能：

- **统计概览卡片**：总里程、总时长、平均速度
- **图表展示**：柱状图（行驶里程）、折线图（在线率趋势）、饼图（报警分布）
- **时间/维度筛选**：时间范围选择 + 日/周/月切换
- **数据导出**：导出为 Excel 文件

---

# 二、接口调用总览

| 接口 | 方法 | 路径 | 用途 |
|------|------|------|------|
| STS-001 | GET | /api/v1/stats/travel | 行驶统计 |
| STS-002 | GET | /api/v1/stats/online-rate | 在线率统计 |
| STS-003 | GET | /api/v1/stats/alarm | 报警统计 |
| STS-004 | GET | /api/v1/stats/export | 导出Excel |

---

# 三、页面设计

## 3.1 布局

```
┌─────────────────────────────────────────────────────────────────┐
│  统计报表                                                       │
├─────────────────────────────────────────────────────────────────┤
│  时间范围 [2026-04-01] ~ [2026-04-12]  统计维度 [日▼]  [查询]    │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────┐ ┌──────┐ ┌──────┐                                     │
│  │总里程 │ │总时长 │ │均速  │                                     │
│  │2856km│ │128h  │ │45km/h│                                     │
│  └──────┘ └──────┘ └──────┘                                     │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ 📊 每日行驶里程 (柱状图)                                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│  ┌──────────────────────┐ ┌──────────────────────┐             │
│  │ 📈 在线率趋势         │ │ 🥧 报警类型分布       │             │
│  └──────────────────────┘ └──────────────────────┘             │
│                                              [导出 Excel]        │
└─────────────────────────────────────────────────────────────────┘
```

---

# 四、组件设计

## 4.1 组件树

```
StatsReport/
├── FilterBar                    # 时间范围 + 维度 + 查询 + 导出
│   ├── RangePicker[时间]
│   ├── Select[维度]
│   └── Button[查询/导出]
├── SummaryCards                 # 统计概览
│   └── Card[总里程/总时长/均速]
├── TravelChart                  # 行驶里程柱状图
│   └── Chart (ECharts/Recharts)
├── OnlineRateChart              # 在线率折线图
│   └── Chart (ECharts/Recharts)
└── AlarmPieChart                # 报警分布饼图
    └── Chart (ECharts/Recharts)
```

## 4.2 图表库选择

| 方案 | 说明 |
|------|------|
| ECharts (echarts-for-react) | 功能最全，社区成熟 |
| Ant Design Charts (@ant-design/charts) | Ant Design 生态，API 简洁 |

**推荐**：Ant Design Charts（与 Ant Design 5.x 配合一致）

---

# 五、TypeScript 类型定义

```typescript
interface TravelStatsVO {
  summary: {
    totalDistance: number;
    totalDuration: number;
    avgSpeed: number;
    vehicleCount: number;
  };
  daily: { date: string; distance: number; duration: number; vehicleCount: number }[];
}

interface OnlineRateStatsVO {
  avgOnlineRate: number;
  trend: { date: string; onlineRate: number }[];
}

interface AlarmStatsVO {
  total: number;
  byType: { alarmType: number; alarmTypeName: string; count: number }[];
  byDate: { date: string; count: number }[];
}
```

---

## 变更记录

| 版本 | 日期 | 修改人 | 变更内容 |
|------|------|--------|---------|
| v1.0.0 | 2026-04-12 | 前端开发（AI） | 初始版本 |
