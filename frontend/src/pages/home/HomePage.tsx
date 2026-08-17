import React from 'react';
import { Card, Col, Row, Descriptions, Tag, Typography, Space, Alert } from 'antd';
import { Link } from 'react-router-dom';
import { useUserStore } from '../../stores/userStore';
import StatCard from '../../components/common/StatCard';
import { userApi } from '../../services/userApi';
import { roleApi } from '../../services/roleApi';
import { menuApi } from '../../services/menuApi';
import { deptApi } from '../../services/deptApi';

const { Paragraph, Text } = Typography;

/** 统计卡片取数：只取 total，失败时 StatCard 显示占位符 */
const countUsers = async () => (await userApi.list({ pageNum: 1, pageSize: 1 })).data.total;
const countRoles = async () => (await roleApi.list({ pageNum: 1, pageSize: 1 })).data.total;

/** 树形接口无 total，递归统计节点数 */
const countTree = (nodes: { children?: unknown[] }[]): number =>
  nodes.reduce(
    (sum, n) => sum + 1 + countTree((n.children ?? []) as { children?: unknown[] }[]),
    0,
  );
const countMenus = async () => countTree((await menuApi.tree()).data ?? []);
const countDepts = async () => countTree((await deptApi.tree()).data ?? []);

/** 系统管理快捷入口。工作台属「业务系统」Layout，侧栏看不到系统菜单，这里给直达链接 */
const ADMIN_LINKS: { to: string; label: string; permission?: string }[] = [
  { to: '/system/tenants', label: '租户管理', permission: 'system:tenant:list' },
  { to: '/system/users', label: '用户管理', permission: 'system:user:list' },
  { to: '/system/roles', label: '角色管理', permission: 'system:role:list' },
  { to: '/system/menu', label: '菜单管理', permission: 'system:menu:list' },
  { to: '/system/dept', label: '部门管理', permission: 'system:dept:list' },
  { to: '/system/dict', label: '字典管理', permission: 'system:dict:list' },
  { to: '/monitor/operlog', label: '操作日志', permission: 'system:operlog:list' },
  { to: '/monitor/loginlog', label: '登录日志', permission: 'system:loginlog:list' },
  { to: '/monitor/online', label: '在线用户', permission: 'system:online:list' },
  { to: '/monitor-center/redis', label: 'Redis 监控', permission: 'monitor:redis:info' },
];

/**
 * 工作台首页 —— 脚手架自带的落地页。
 * 新项目通常做两件事之一：
 *   1) 直接替换本文件内容为自己的业务概览；
 *   2) 新建业务首页并在 utils/menuMapper.ts 的 COMPONENT_MAP 中登记，
 *      再写一条迁移改掉 sys_menu 里首页那条的 component。
 */
const HomePage: React.FC = () => {
  const userInfo = useUserStore((s) => s.userInfo);
  const hasPermission = useUserStore((s) => s.hasPermission);

  const links = ADMIN_LINKS.filter((l) => !l.permission || hasPermission(l.permission));

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="primary" label="用户数" fetcher={countUsers} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="info" label="角色数" fetcher={countRoles} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="success" label="菜单/权限点" fetcher={countMenus} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="warning" label="部门数" fetcher={countDepts} />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="当前登录身份">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="账号">{userInfo?.username ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="姓名">{userInfo?.nickname ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="部门">{userInfo?.deptName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="角色">
                {(userInfo?.roles ?? []).map((r) => (
                  <Tag color="blue" key={r.id}>
                    {r.roleName}（{r.roleCode}）
                  </Tag>
                ))}
              </Descriptions.Item>
              <Descriptions.Item label="权限点">
                <Text type="secondary">共 {userInfo?.permissions?.length ?? 0} 个</Text>
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="系统管理快捷入口" extra={<Text type="secondary">顶栏齿轮图标可切换到系统管理布局</Text>}>
            <Space size={[8, 8]} wrap>
              {links.map((l) => (
                <Link key={l.to} to={l.to}>
                  <Tag color="processing" style={{ cursor: 'pointer', margin: 0 }}>
                    {l.label}
                  </Tag>
                </Link>
              ))}
            </Space>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card title="脚手架自带能力">
            <Paragraph style={{ marginBottom: 8 }}>
              多租户隔离 · RBAC 权限（用户/角色/菜单/部门/字典）· 数据权限五档 · 操作与登录日志 ·
              在线用户强制下线 · JWT + Redis 黑名单 · 限流 / 防重提交 / 全链路 TraceId · Redis 监控
            </Paragraph>
            <Alert
              type="info"
              showIcon
              message="开始一个新模块"
              description={
                <span>
                  后端：<Text code>backend/gentry-business/src/main/java/com/gentry/</Text> 下新建包，
                  照 <Text code>rbac/dept</Text> 的 controller/service/mapper/entity/dto/vo 分层照抄；
                  前端：<Text code>src/pages/</Text> 下新建页面并在{' '}
                  <Text code>utils/menuMapper.ts</Text> 登记；
                  权限点：写一条 Flyway 迁移插 <Text code>sys_menu</Text> + <Text code>sys_role_menu</Text>。
                  详见 <Text code>doc/guide/RBAC模块开发指南.md</Text>。
                </span>
              }
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
};

export default HomePage;
