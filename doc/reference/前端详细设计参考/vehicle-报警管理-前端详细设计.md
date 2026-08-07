# 前端详细设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档名称 | 报警管理 前端详细设计文档 |
| 对应后端模块 | 报警管理（模块标识：alarm） |
| 参考文档 | `doc/design/modules/vehicle/modules/报警管理/后端详细设计.md` |
| 静态页面 | 暂无（待创建） |
| 版本 | v1.0.0 |
| 创建日期 | 2026-04-12 |
| 负责人 | 前端开发（AI辅助） |

---

# 一、模块概述

## 1.1 模块功能描述

本模块前端实现**报警管理**功能：

- **报警事件列表**：分页展示报警记录，支持按类型/状态/时间筛选
- **状态 Tab**：待处理/已确认/已忽略/已处理，显示计数
- **报警详情**：弹窗展示报警详细信息（车辆/位置/时间）
- **报警处理**：确认/忽略/处理完成操作
- **报警规则配置**：管理超速/离线等报警阈值

## 1.2 权限

| 角色 | 操作 | 权限标识 |
|------|------|---------|
| 运维人员 | 查看+处理报警 | alarm:list / alarm:handle |
| 系统管理员 | 规则配置 | alarm:config |

---

# 二、接口调用总览

| 接口 | 方法 | 路径 | 用途 |
|------|------|------|------|
| ALM-001 | GET | /api/v1/alarms | 事件列表 |
| ALM-002 | GET | /api/v1/alarms/{id} | 报警详情 |
| ALM-003 | PUT | /api/v1/alarms/{id}/handle | 处理报警 |
| ALM-004 | GET | /api/v1/alarms/stats | 统计计数 |
| ALM-005 | GET | /api/v1/alarm-rules | 规则列表 |
| ALM-006 | PUT | /api/v1/alarm-rules | 更新规则 |

---

# 三、页面设计

## 3.1 页面布局

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  报警管理                                                [报警规则]          │
├─────────────────────────────────────────────────────────────────────────────┤
│  报警类型[全部▼] 状态[全部▼] 时间[___]~[___] 车牌[____]  [查询] [重置]        │
├─────────────────────────────────────────────────────────────────────────────┤
│ [待处理(12)] [已确认(5)] [已忽略(3)] [已处理(28)]                            │
├────┬──────┬──────┬──────┬────────┬──────────┬──────┬──────┬───────────────┤
│序号│类型  │车牌号│车辆名│报警时间 │报警位置   │状态  │处理人 │ 操作          │
├────┼──────┼──────┼──────┼────────┼──────────┼──────┼──────┼───────────────┤
│ 1  │🔴超速│浙A123│物流01│04-12 14│文三路    │待处理│ -    │[详情][处理]   │
│ 2  │⚪离线│浙A678│物流02│04-12 13│ -        │已确认│张三  │[详情]        │
└────┴──────┴──────┴──────┴────────┴──────────┴──────┴──────┴───────────────┘
                                                             < 1 2 3 >
```

---

# 四、操作流程

## 4.1 报警处理

```
用户点击「处理」
  → 弹出处理弹窗（Radio选择：确认/忽略/处理完成 + 备注）
  → 提交 → 调用 ALM-003
  → 成功 → message.success + 刷新列表 + 刷新统计
```

## 4.2 报警详情

```
用户点击「详情」
  → 弹出详情弹窗
  → 展示：车辆信息、报警类型、报警值、位置、时间、处理记录
```

## 4.3 规则配置

```
用户点击「报警规则」
  → 弹出规则配置弹窗
  → 表单：超速阈值 + 离线时长 + 开关
  → 保存 → 调用 ALM-006
  → 成功 → message.success + 关闭弹窗
```

---

# 五、组件设计

## 5.1 组件树

```
AlarmManagement/
├── SearchBar                    # 筛选条件
│   ├── Select[报警类型]
│   ├── Select[处理状态]
│   ├── RangePicker[时间]
│   ├── Input[车牌号]
│   └── Button[查询/重置]
├── StatusTabs                   # 状态 Tab 栏
│   └── Tabs                     # 待处理/已确认/已忽略/已处理
├── AlarmTable                   # 报警事件表格
│   └── Table
├── HandleModal                  # 处理弹窗
│   ├── Radio.Group[操作]
│   └── TextArea[备注]
├── DetailModal                  # 详情弹窗
│   └── Descriptions
└── RuleModal                    # 规则配置弹窗
    ├── Form[超速阈值]
    ├── Form[离线时长]
    └── Button[保存]
```

## 5.2 表格列定义

| 列 | dataIndex | 宽度 | 渲染 |
|------|-----------|------|------|
| 序号 | index | 60px | 自增 |
| 报警类型 | alarmType | 120px | Badge: 🔴超速/⚪离线/🟡围栏/SOS |
| 车牌号 | plateNumber | 140px | 文本 |
| 车辆名 | vehicleName | 120px | 文本 |
| 报警时间 | alarmTime | 170px | YYYY-MM-DD HH:mm |
| 报警位置 | location | 200px | Tooltip |
| 处理状态 | status | 100px | Tag: 待处理=red, 已确认=blue, 已忽略=gray, 已处理=green |
| 处理人 | handleBy | 100px | 文本 |
| 操作 | — | 140px | [详情] + 待处理时显示[处理] |

---

# 六、TypeScript 类型定义

```typescript
interface AlarmVO {
  alarmId: number;
  deviceSn: string;
  vehicleId: number | null;
  plateNumber: string;
  vehicleName: string;
  alarmType: number;
  alarmTypeName: string;
  alarmTime: string;
  alarmValue: string | null;
  longitude: number | null;
  latitude: number | null;
  location: string | null;
  status: number;
  statusName: string;
  handleBy: number | null;
  handleTime: string | null;
  handleRemark: string | null;
}

interface AlarmStatsVO {
  pending: number;
  confirmed: number;
  ignored: number;
  processed: number;
}

interface AlarmRuleVO {
  alarmType: number;
  alarmTypeName: string;
  rules: { paramKey: string; paramValue: string; enabled: boolean }[];
}
```

---

## 变更记录

| 版本 | 日期 | 修改人 | 变更内容 |
|------|------|--------|---------|
| v1.0.0 | 2026-04-12 | 前端开发（AI） | 初始版本 |
