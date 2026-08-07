import { useState, type ReactNode } from 'react';
import dayjs from 'dayjs';
import {
  Button, Input, Select, Card, Typography, DatePicker, Switch,
  Badge, Tag, Tabs, Modal, Alert, Progress, Tooltip, Popover, Dropdown,
  Avatar, Collapse, Timeline, Pagination, Space, Divider,
  Checkbox, Radio, InputNumber, Slider, Upload,
  Table, Breadcrumb, Statistic, Steps, Descriptions, List,
  Drawer, Result, Empty, Spin,
} from 'antd';
import {
  PlusOutlined, SearchOutlined, BellOutlined, UserOutlined, DownOutlined,
  InboxOutlined, UploadOutlined, HomeOutlined,
} from '@ant-design/icons';
import { PageSelect, SweetAlert, type SweetAlertType } from '../../components/pro';
import type { ApiResult, PageResult, PageQuery } from '../../types/api';

const { Title, Text } = Typography;
const { Dragger } = Upload;
const { RangePicker } = DatePicker;

/** 左侧锚点导航分类 */
const NAV: { group: string; items: [string, string][] }[] = [
  { group: '基础', items: [['buttons', '按钮 Button'], ['typography', '排版 Typography'], ['divider', '分割线 Divider']] },
  { group: '表单', items: [['inputs', '输入框 Input'], ['select', '选择器 Select'], ['pageSelect', '分页下拉 PageSelect ★'], ['datepicker', '日期 DatePicker'], ['switch', '开关 Switch'], ['checkbox', '复选 Checkbox'], ['radio', '单选 Radio'], ['inputNumber', '数字 InputNumber'], ['slider', '滑块 Slider'], ['upload', '上传 Upload']] },
  { group: '数据展示', items: [['table', '表格 Table ★'], ['statistic', '统计数字 Statistic'], ['descriptions', '描述 Descriptions ★'], ['list', '列表 List ★'], ['badge', '徽标 Badge'], ['tag', '标签 Tag'], ['avatar', '头像 Avatar'], ['collapse', '折叠 Collapse'], ['timeline', '时间线 Timeline']] },
  { group: '反馈', items: [['modal', '弹窗 Modal'], ['sweetalert', 'SweetAlert 弹窗 ★'], ['drawer', '抽屉 Drawer ★'], ['result', '结果页 Result ★'], ['empty', '空状态 Empty ★'], ['spin', '加载 Spin ★'], ['alert', '警告 Alert'], ['progress', '进度 Progress'], ['tooltip', '提示 Tooltip'], ['popover', '气泡 Popover'], ['dropdown', '下拉 Dropdown']] },
  { group: '导航', items: [['breadcrumb', '面包屑 Breadcrumb'], ['tabs', '标签页 Tabs'], ['steps', '步骤条 Steps'], ['pagination', '分页 Pagination ★']] },
];

function Section({ id, title, desc, children }: { id: string; title: string; desc?: string; children: ReactNode }) {
  return (
    <section id={id} style={{ marginBottom: 40, scrollMarginTop: 16 }}>
      <Title level={4} style={{ color: '#32325d', marginBottom: 4 }}>{title}</Title>
      {desc && <Text type="secondary" style={{ fontSize: 12 }}>{desc}</Text>}
      <div style={{ marginTop: 12, padding: 24, background: '#fff', borderRadius: 6, boxShadow: '0 0 2rem 0 rgba(136,152,170,.15)' }}>
        {children}
      </div>
    </section>
  );
}

/**
 * Argon 组件参考页（开发用，对照 Argon 文档）
 * 左侧锚点分类导航 + 右侧各组件 variant 展示。
 * 改 argonTheme/argon.less 后 HMR 秒级回显。访问 /dev/style（免登录）
 */
export default function StylePreviewPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<{ vehicleId: number; plateNumber: string; vehicleType: number } | null>(null);
  const [sweet, setSweet] = useState<{ type: SweetAlertType; title: string; content?: ReactNode } | null>(null);

  // PageSelect mock service：23 条假车辆，支持 plateNumber 远程搜索 + 分页
  const vehicleMockService = async (params: PageQuery): Promise<ApiResult<PageResult<{ vehicleId: number; plateNumber: string; vehicleType: number }>>> => {
    const all = Array.from({ length: 23 }, (_, i) => ({
      vehicleId: i + 1,
      plateNumber: `京A·${10000 + i}`,
      vehicleType: (i % 4) + 1,
    }));
    const pn = (params.pageNum as number) ?? 1;
    const ps = (params.pageSize as number) ?? 5;
    const f = params.plateNumber
      ? all.filter((v) => v.plateNumber.includes(String(params.plateNumber)))
      : all;
    return {
      code: 0,
      data: {
        list: f.slice((pn - 1) * ps, pn * ps),
        total: f.length,
        pageNum: pn,
        pageSize: ps,
        pages: Math.ceil(f.length / ps),
      },
    };
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', background: '#f8f9fe' }}>
      {/* 左侧锚点导航 */}
      <aside style={{ width: 210, position: 'sticky', top: 0, height: '100vh', overflowY: 'auto', padding: '24px 0', background: '#fff', borderRight: '1px solid #e9ecef', flexShrink: 0 }}>
        <div style={{ padding: '0 16px 8px' }}>
          <Title level={4} style={{ color: '#32325d', marginBottom: 0 }}>组件参考</Title>
          <Text type="secondary" style={{ fontSize: 11 }}>/dev/style</Text>
        </div>
        {NAV.map((g) => (
          <div key={g.group} style={{ marginTop: 12, padding: '0 16px' }}>
            <div style={{ color: '#8898aa', fontSize: 10, textTransform: 'uppercase', letterSpacing: '.06em', fontWeight: 700, marginBottom: 4 }}>{g.group}</div>
            {g.items.map(([id, name]) => (
              <a key={id} href={`#${id}`} style={{ display: 'block', padding: '3px 0', color: '#525f7f', fontSize: 13 }}>{name}</a>
            ))}
          </div>
        ))}
      </aside>

      {/* 右侧组件展示 */}
      <main style={{ flex: 1, padding: 32, maxWidth: 920 }}>
        <Title level={3} style={{ color: '#32325d' }}>Argon 组件参考</Title>
        <Text type="secondary">对照 demos.creative-tim.com/argon-dashboard-pro-react — 改 argonTheme/argon.less 后 HMR 秒级回显</Text>

        {/* 基础 */}
        <Section id="buttons" title="按钮 Button" desc="5 色渐变(ps-btn-gradient-*) + neutral + danger + size + 图标，hover 上移 -1px">
          <Space wrap>
            <Button type="primary">Primary</Button>
            <Button type="primary" className="ps-btn-gradient-primary">渐变 Primary</Button>
            <Button type="primary" className="ps-btn-gradient-info">渐变 Info</Button>
            <Button type="primary" className="ps-btn-gradient-success">渐变 Success</Button>
            <Button type="primary" className="ps-btn-gradient-danger">渐变 Danger</Button>
            <Button type="primary" className="ps-btn-gradient-default">渐变 Default</Button>
            <Button>Default</Button>
            <Button className="ps-btn-neutral">Neutral</Button>
            <Button type="primary" danger>Danger</Button>
            <Button disabled>Disabled</Button>
            <Button type="link">Link</Button>
            <Button type="primary" icon={<PlusOutlined />}>图标</Button>
            <Button type="primary" size="small">Small</Button>
            <Button type="primary" size="large">Large</Button>
          </Space>
        </Section>

        <Section id="typography" title="排版 Typography">
          <Title level={4} style={{ color: '#32325d' }}>标题 H4</Title>
          <p><Text>正文 Text — gray-700 #525f7f</Text></p>
          <p><Text type="secondary">次要 secondary — gray-500</Text></p>
          <p><Text strong>加粗 strong</Text></p>
        </Section>

        <Section id="divider" title="分割线 Divider"><Divider /></Section>

        {/* 表单 */}
        <Section id="inputs" title="输入框 Input" desc="focus 有柔和阴影（cubic-bezier 过渡）">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Input placeholder="普通输入框" />
            <Input placeholder="带前缀" prefix={<SearchOutlined />} />
            <Input.Password placeholder="密码框" />
            <Input.TextArea placeholder="多行文本" rows={2} />
          </Space>
        </Section>

        <Section id="select" title="选择器 Select">
          <Select style={{ width: '100%' }} placeholder="请选择" options={[{ label: '选项一', value: 1 }, { label: '选项二', value: 2 }]} />
        </Section>

        <Section id="pageSelect" title="分页下拉 PageSelect ★" desc="大数据量选择：点开是分页 table（远程搜索），不全量塞">
          <Space direction="vertical" style={{ width: '100%' }}>
            <PageSelect
              service={vehicleMockService}
              columns={[
                { title: '车牌号', dataIndex: 'plateNumber', key: 'plateNumber' },
                { title: '类型', dataIndex: 'vehicleType', key: 'vehicleType', width: 80 },
              ]}
              rowKey="vehicleId"
              labelField="plateNumber"
              searchField="plateNumber"
              value={selectedVehicle}
              onChange={setSelectedVehicle}
              placeholder="点我选车辆（23 条数据分页）"
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              当前选中：{selectedVehicle ? `${selectedVehicle.plateNumber}（类型 ${selectedVehicle.vehicleType}）` : '无'}
            </Text>
          </Space>
        </Section>

        <Section id="datepicker" title="日期 DatePicker / RangePicker" desc="focus 柔和阴影 + cubic-bezier 过渡（与 Input 同款 Argon 风格）">
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Space wrap align="center">
              <Text type="secondary" style={{ fontSize: 12, width: 64 }}>单日期</Text>
              <DatePicker style={{ width: 200 }} />
            </Space>
            <Space wrap align="center">
              <Text type="secondary" style={{ fontSize: 12, width: 64 }}>日期范围</Text>
              <RangePicker style={{ width: 340 }} />
            </Space>
            <Space wrap align="center">
              <Text type="secondary" style={{ fontSize: 12, width: 64 }}>含时间</Text>
              <RangePicker showTime style={{ width: 400 }} />
            </Space>
            <Space wrap align="center">
              <Text type="secondary" style={{ fontSize: 12, width: 64 }}>预设范围</Text>
              <RangePicker
                ranges={{
                  '今天': [dayjs().startOf('day'), dayjs().endOf('day')],
                  '近7天': [dayjs().subtract(6, 'day').startOf('day'), dayjs().endOf('day')],
                  '近30天': [dayjs().subtract(29, 'day').startOf('day'), dayjs().endOf('day')],
                }}
                style={{ width: 340 }}
              />
            </Space>
          </Space>
        </Section>

        <Section id="switch" title="开关 Switch">
          <Space><Switch defaultChecked /><Switch /></Space>
        </Section>

        <Section id="checkbox" title="复选框 Checkbox" desc="checked 色 primary（$component-active-bg）+ hover 边框灰化（$gray-300）">
          <Space direction="vertical">
            <Space>
              <Checkbox defaultChecked>默认选中</Checkbox>
              <Checkbox>普通</Checkbox>
              <Checkbox disabled>禁用</Checkbox>
              <Checkbox disabled checked>选中禁用</Checkbox>
            </Space>
            <Checkbox.Group
              options={[
                { label: '苹果', value: 'apple' },
                { label: '香蕉', value: 'banana' },
                { label: '橙子', value: 'orange' },
              ]}
              defaultValue={['apple', 'orange']}
            />
            <Checkbox indeterminate>半选状态</Checkbox>
          </Space>
        </Section>

        <Section id="radio" title="单选 Radio" desc="checked 色 primary + hover 边框灰化">
          <Space direction="vertical">
            <Space>
              <Radio defaultChecked>默认</Radio>
              <Radio>普通</Radio>
              <Radio disabled>禁用</Radio>
              <Radio disabled checked>选中禁用</Radio>
            </Space>
            <Radio.Group defaultValue={1}>
              <Radio value={1}>A</Radio>
              <Radio value={2}>B</Radio>
              <Radio value={3}>C</Radio>
            </Radio.Group>
            <Radio.Group defaultValue="beijing" buttonStyle="solid">
              <Radio.Button value="shanghai">上海</Radio.Button>
              <Radio.Button value="beijing">北京</Radio.Button>
              <Radio.Button value="guangzhou">广州</Radio.Button>
            </Radio.Group>
          </Space>
        </Section>

        <Section id="inputNumber" title="数字输入 InputNumber" desc="40px 高 + 4px 圆角 + focus 柔和阴影（与 Input 同款 Argon 风格）">
          <Space wrap>
            <InputNumber defaultValue={5} />
            <InputNumber min={0} max={10} defaultValue={3} />
            <InputNumber defaultValue={0} step={0.1} />
            <InputNumber addonBefore="$" addonAfter="元" defaultValue={100} />
            <InputNumber disabled defaultValue={5} />
          </Space>
        </Section>

        <Section id="slider" title="滑块 Slider" desc="Argon noUi 复刻：5px rail + 圆形 primary handle + 87deg 渐变 track + hover 放大">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Slider defaultValue={30} />
            <Slider
              defaultValue={50}
              marks={{ 0: '0°C', 25: '25°C', 50: '50°C', 75: '75°C', 100: '100°C' }}
            />
            <Slider range defaultValue={[20, 60]} />
            <Slider defaultValue={40} disabled />
          </Space>
        </Section>

        <Section id="upload" title="上传 Upload" desc="Dragger 虚线 + hover 强调（Argon dropzone 风：拖入 primary 边框）">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Dragger>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p className="ant-upload-hint">支持单文件或多文件上传，严禁上传公司敏感资料</p>
            </Dragger>
            <Upload
              defaultFileList={[
                { uid: '1', name: 'vehicle-template.xlsx', status: 'done', url: 'https://example.com' },
                { uid: '2', name: 'driver-import.csv', status: 'error' },
              ]}
            >
              <Button icon={<UploadOutlined />}>普通上传按钮</Button>
            </Upload>
          </Space>
        </Section>

        {/* 数据展示 */}
        <Section id="table" title="表格 Table ★" desc="Argon card-table 包裹风：thead gray-100 底 + gray-600 uppercase 字 / body .8125rem / 行 hover gray-100（ProTable 内置 Table 自动受益）">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>基础（Card 包裹 + 表头 uppercase）</Text>
              <Card>
                <Table
                  size="middle"
                  pagination={false}
                  rowKey="vehicleId"
                  dataSource={[
                    { vehicleId: 1, plateNumber: '京A·10001', vehicleType: '乘用车', online: true },
                    { vehicleId: 2, plateNumber: '京A·10002', vehicleType: '商用车', online: false },
                    { vehicleId: 3, plateNumber: '京A·10003', vehicleType: '乘用车', online: true },
                  ]}
                  columns={[
                    { title: '车牌号', dataIndex: 'plateNumber', key: 'plateNumber' },
                    { title: '类型', dataIndex: 'vehicleType', key: 'vehicleType' },
                    { title: '状态', dataIndex: 'online', key: 'online', render: (v) => (v ? <Tag color="green">在线</Tag> : <Tag>离线</Tag>) },
                  ]}
                />
              </Card>
            </div>
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>无边框变体（borderless）+ 排序</Text>
              <Table
                size="middle"
                pagination={false}
                rowKey="vehicleId"
                dataSource={[
                    { vehicleId: 1, plateNumber: '京A·10001', vehicleType: '乘用车', online: true },
                    { vehicleId: 2, plateNumber: '京A·10002', vehicleType: '商用车', online: false },
                    { vehicleId: 3, plateNumber: '京A·10003', vehicleType: '乘用车', online: true },
                  ]}
                columns={[
                  { title: '车牌号', dataIndex: 'plateNumber', key: 'plateNumber', sorter: (a, b) => a.plateNumber.localeCompare(b.plateNumber) },
                  { title: '类型', dataIndex: 'vehicleType', key: 'vehicleType' },
                ]}
              />
            </div>
          </Space>
        </Section>

        <Section id="statistic" title="统计数字 Statistic" desc="Argon 数据卡风：标题 uppercase gray-600 + 数字 gray-800 大字号（30px / 700 字重）">
          <Space size="large" wrap>
            <Statistic title="车辆总数" value={1280} suffix="辆" />
            <Statistic title="在线车辆" value={946} suffix="辆" valueStyle={{ color: '#2dce89' }} />
            <Statistic title="报警中" value={13} suffix="起" valueStyle={{ color: '#f5365c' }} />
          </Space>
        </Section>

        <Section id="descriptions" title="描述列表 Descriptions ★" desc="Argon 数据卡 + table thead 风：label gray-100 底 + gray-600 uppercase / 内容 gray-700 / 标题 gray-800 700">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>默认（无边框 + 紧凑）— 车辆基本信息</Text>
              <Descriptions title="车辆基本信息" column={2}>
                <Descriptions.Item label="车牌号">京A·10001</Descriptions.Item>
                <Descriptions.Item label="车型">乘用车</Descriptions.Item>
                <Descriptions.Item label="状态"><Tag color="green">在线</Tag></Descriptions.Item>
                <Descriptions.Item label="所属车队">华东运营一队</Descriptions.Item>
                <Descriptions.Item label="设备 SN">DEV202600001</Descriptions.Item>
                <Descriptions.Item label="协议类型">JT808-2019</Descriptions.Item>
              </Descriptions>
            </div>
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>带边框（bordered）— Argon 数据卡式 label 底</Text>
              <Descriptions title="设备详情" bordered column={2}>
                <Descriptions.Item label="设备 SN">DEV202600001</Descriptions.Item>
                <Descriptions.Item label="协议">JT808-2019</Descriptions.Item>
                <Descriptions.Item label="固件版本">v2.3.1</Descriptions.Item>
                <Descriptions.Item label="入网时间">2026-07-15 09:23</Descriptions.Item>
                <Descriptions.Item label="最后上报">2026-08-01 14:08</Descriptions.Item>
                <Descriptions.Item label="信号强度">良好</Descriptions.Item>
              </Descriptions>
            </div>
          </Space>
        </Section>

        <Section id="list" title="列表 List ★" desc="Argon list-group 风：item 12/20 padding + 底边 gray-200 + hover gray-100 + 内容 gray-700 + Meta 标题 gray-800 / 描述 gray-600">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>基础列表（split 分割线 + hover 高亮）</Text>
              <List
                header={<div>最近报警</div>}
                footer={<div>共 3 条未处理</div>}
                split
                dataSource={[
                  { id: 1, title: '京A·10001 超速报警', desc: '车速 132 km/h，限速 100，2026-08-01 14:02' },
                  { id: 2, title: '京A·10002 疲劳驾驶', desc: '连续驾驶 4 小时 12 分，2026-08-01 13:47' },
                  { id: 3, title: '京A·10003 越范围', desc: '偏离规定路线 3.2 km，2026-08-01 12:35' },
                ]}
                renderItem={(item) => (
                  <List.Item>
                    <List.Item.Meta
                      avatar={<Avatar style={{ background: '#f5365c' }}>{item.id}</Avatar>}
                      title={item.title}
                      description={item.desc}
                    />
                  </List.Item>
                )}
              />
            </div>
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>可操作列表（actions + bordered）— hover 看效果</Text>
              <List
                bordered
                dataSource={[
                  { id: 1, name: '华东运营一队', count: 28 },
                  { id: 2, name: '华南运营二队', count: 17 },
                  { id: 3, name: '华北运营三队', count: 12 },
                ]}
                renderItem={(item) => (
                  <List.Item actions={[<a key="edit">编辑</a>, <a key="del">删除</a>]}>
                    <List.Item.Meta title={<a>{item.name}</a>} description={`在线车辆 ${item.count} 辆`} />
                  </List.Item>
                )}
              />
            </div>
          </Space>
        </Section>

        <Section id="badge" title="徽标 Badge">
          <Space size="large">
            <Badge count={5}><Avatar shape="square" icon={<BellOutlined />} /></Badge>
            <Badge count={0} showZero><Avatar shape="square" icon={<BellOutlined />} /></Badge>
            <Badge dot><Avatar shape="square" icon={<UserOutlined />} /></Badge>
            <Badge status="success" text="成功" />
            <Badge status="error" text="错误" />
          </Space>
        </Section>

        <Section id="tag" title="标签 Tag">
          <Space wrap>
            <Tag color="magenta">magenta</Tag>
            <Tag color="blue">blue</Tag>
            <Tag color="green">green</Tag>
            <Tag color="orange">orange</Tag>
            <Tag color="red">red</Tag>
            <Tag closable>可关闭</Tag>
          </Space>
        </Section>

        <Section id="avatar" title="头像 Avatar">
          <Space size="large">
            <Avatar icon={<UserOutlined />} />
            <Avatar size="large" icon={<UserOutlined />} />
            <Avatar size={40}>A</Avatar>
            <Avatar.Group>
              <Avatar icon={<UserOutlined />} />
              <Avatar icon={<UserOutlined />} />
              <Avatar icon={<UserOutlined />} />
            </Avatar.Group>
          </Space>
        </Section>

        <Section id="collapse" title="折叠面板 Collapse">
          <Collapse items={[{ key: '1', label: '面板一', children: <p>面板一内容</p> }, { key: '2', label: '面板二', children: <p>面板二内容</p> }]} />
        </Section>

        <Section id="timeline" title="时间线 Timeline">
          <Timeline items={[{ color: 'green', children: '步骤一 完成' }, { color: 'blue', children: '步骤二 进行中' }, { color: 'gray', children: '步骤三 待办' }]} />
        </Section>

        {/* 反馈 */}
        <Section id="modal" title="弹窗 Modal">
          <Button type="primary" onClick={() => setModalOpen(true)}>打开 Modal</Button>
          <Modal open={modalOpen} onOk={() => setModalOpen(false)} onCancel={() => setModalOpen(false)} title="Modal 标题">
            <p>Modal 内容——圆角、阴影、header/footer 分隔待 Argon 对齐</p>
          </Modal>
        </Section>

        <Section id="sweetalert" title="SweetAlert 弹窗 ★" desc="Argon sweet-alert 风格：中央大圆形渐变图标(type 色) + 标题 + 正文 + 渐变确认/取消按钮">
          <Space wrap>
            <Button className="ps-btn-gradient-success" onClick={() => setSweet({ type: 'success', title: '操作成功', content: '数据已保存' })}>Success</Button>
            <Button className="ps-btn-gradient-danger" onClick={() => setSweet({ type: 'error', title: '出错了', content: '请稍后重试' })}>Error</Button>
            <Button className="ps-btn-gradient-warning" onClick={() => setSweet({ type: 'warning', title: '确定删除？', content: '此操作不可恢复' })}>Warning</Button>
            <Button className="ps-btn-gradient-info" onClick={() => setSweet({ type: 'info', title: '提示', content: '这是一条信息' })}>Info</Button>
          </Space>
          <SweetAlert
            open={!!sweet}
            type={sweet?.type ?? 'success'}
            title={sweet?.title ?? ''}
            content={sweet?.content}
            confirmText="确认"
            cancelText="取消"
            onConfirm={() => setSweet(null)}
            onCancel={() => setSweet(null)}
          />
        </Section>

        <Section id="drawer" title="抽屉 Drawer ★" desc="Argon 用 Modal 代 Drawer：圆角 7 + Modal 浮层阴影 + mask .16 + gray-800 标题 700">
          <Button type="primary" onClick={() => setDrawerOpen(true)}>打开 Drawer</Button>
          <Drawer
            open={drawerOpen}
            title="Drawer 标题"
            width={420}
            onClose={() => setDrawerOpen(false)}
            extra={<Space><Button onClick={() => setDrawerOpen(false)}>取消</Button><Button type="primary" onClick={() => setDrawerOpen(false)}>确定</Button></Space>}
          >
            <p style={{ marginTop: 0 }}>Drawer 内容——白底 + 圆角 7 + Modal 同款浮层阴影 + gray-800 标题 700</p>
            <p>抽屉用于详情面板、复杂表单等不需中断上下文的场景（车辆/司机详情、批量编辑）。</p>
          </Drawer>
        </Section>

        <Section id="result" title="结果页 Result ★" desc="Argon 用 SweetAlert 代 Result：中央 80px 渐变圆 + 白图标 + gray-800 标题 700 + gray-700 副标题">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Result
              status="success"
              title="提交成功"
              subTitle="订单编号 2026080100001 已生成，预计 3 个工作日内发货。"
              extra={[
                <Button type="primary" key="go">查看订单</Button>,
                <Button key="back">返回列表</Button>,
              ]}
            />
            <Divider />
            <Result
              status="error"
              title="提交失败"
              subTitle="请检查网络后重试，错误码 500。"
              extra={[<Button type="primary" key="retry">重试</Button>]}
            />
          </Space>
        </Section>

        <Section id="empty" title="空状态 Empty ★" desc="Argon 文字简洁风：大图标 gray-400 + gray-600 紧凑描述">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Empty description="暂无车辆数据" />
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="简洁变体（无数据）" />
            <Card>
              <Empty description="表格空数据场景" />
            </Card>
          </Space>
        </Section>

        <Section id="spin" title="加载 Spin ★" desc="Argon loader：primary 色 + 简洁 size lg/sm + 嵌入卡片 loading">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Space size="large" align="center">
              <Spin size="small" />
              <Spin />
              <Spin size="large" />
              <Spin tip="加载中..." />
            </Space>
            <Divider />
            <Card title="嵌入式 loading" extra={<Text type="secondary" style={{ fontSize: 12 }}>Spin 包裹 Card</Text>}>
              <Spin spinning>
                <p>Spin 嵌入卡片加载：半透明白色覆盖 + primary 色 loader（Argon 卡片加载风）。</p>
                <p>卡片内容呈现时遮罩半透明（rgba 255,255,255,.65），是 Argon 数据卡典型加载反馈。</p>
              </Spin>
            </Card>
          </Space>
        </Section>

        <Section id="alert" title="警告 Alert">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Alert type="success" message="成功提示" showIcon />
            <Alert type="info" message="信息提示" showIcon />
            <Alert type="warning" message="警告提示" showIcon />
            <Alert type="error" message="错误提示" showIcon />
          </Space>
        </Section>

        <Section id="progress" title="进度 Progress">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Progress percent={30} />
            <Progress percent={60} status="active" />
            <Progress percent={100} status="success" />
            <Progress type="circle" percent={75} />
          </Space>
        </Section>

        <Section id="tooltip" title="提示 Tooltip">
          <Tooltip title="tooltip 文案——深底圆角小">
            <Button>hover 我</Button>
          </Tooltip>
        </Section>

        <Section id="popover" title="气泡 Popover">
          <Popover content="popover 内容——圆角阴影白底" title="标题">
            <Button>click 我</Button>
          </Popover>
        </Section>

        <Section id="dropdown" title="下拉 Dropdown">
          <Dropdown menu={{ items: [{ key: '1', label: '动作一' }, { key: '2', label: '动作二' }, { key: '3', label: '动作三' }] }}>
            <Button>下拉菜单 <DownOutlined /></Button>
          </Dropdown>
        </Section>

        {/* 导航 */}
        <Section id="breadcrumb" title="面包屑 Breadcrumb" desc="Argon .breadcrumb-links 风：透明底 + $font-size-sm 字号 + gray-600 字色 + 当前页 gray-800 字重强调">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Breadcrumb
              items={[
                { title: <a><HomeOutlined /> 首页</a> },
                { title: <a>车辆管理</a> },
                { title: '车辆列表' },
              ]}
            />
            <Breadcrumb
              items={[
                { title: <a>系统设置</a> },
                { title: <a>权限管理</a> },
                { title: <a>角色</a> },
                { title: '新建角色' },
              ]}
            />
          </Space>
        </Section>

        <Section id="tabs" title="标签页 Tabs">
          <Tabs items={[{ key: '1', label: 'Tab 1', children: <p>内容一</p> }, { key: '2', label: 'Tab 2', children: <p>内容二</p> }, { key: '3', label: 'Tab 3', children: <p>内容三</p> }]} />
        </Section>

        <Section id="steps" title="步骤条 Steps" desc="Argon timeline 风：节点圆 33px + 轴线 gray-200 + 完成段轴线/节点 primary（含完成/进行中/待办三态）">
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <Steps
              current={1}
              items={[
                { title: '设备登记', description: '录入设备基本信息' },
                { title: '协议配置', description: '选择协议插件（808/1078/32960）' },
                { title: '绑定车辆', description: '关联车牌号与司机' },
                { title: '上线运行', description: '设备开始上报数据' },
              ]}
            />
            <Steps
              current={2}
              status="process"
              items={[
                { title: '创建工单', description: '录入工单标题' },
                { title: '指派人员', description: '分配维修工' },
                { title: '执行中', description: '当前步骤' },
                { title: '完成验收' },
              ]}
            />
          </Space>
        </Section>

        <Section id="pagination" title="分页 Pagination ★ 重点精美复刻" desc="待 Argon 对齐：圆形页码 / active 实色 / prev-next 箭头（Argon 标志性漂亮分页）">
          <Space direction="vertical">
            <Pagination defaultCurrent={1} total={50} showTotal={(t) => `共 ${t} 条`} />
            <Pagination defaultCurrent={1} total={50} showSizeChanger showQuickJumper />
          </Space>
        </Section>
      </main>
    </div>
  );
}
