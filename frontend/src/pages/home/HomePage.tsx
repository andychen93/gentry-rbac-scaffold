import React from 'react';
import { Card, Col, Row, Descriptions, Tag, Typography, Space, Alert } from 'antd';
import { Link } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';
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

/**
 * 系统管理快捷入口。工作台属「业务系统」Layout，侧栏看不到系统菜单，这里给直达链接。
 *
 * **label 不再写死中文** —— 原来这些 label 就是菜单名，是 sys_menu.name 的第三份
 * 拷贝（前两份：sys_menu 本身、AppHeader 的 BREADCRUMB_MAP）。现在复用 nav namespace 的
 * key，key 由 permission 去掉最后一段动作派生，与后端 MenuI18nKeyResolver 同规则。
 */
const ADMIN_LINKS: { to: string; navKey: string; permission?: string }[] = [
  { to: '/system/users', navKey: 'menu.system.user', permission: 'system:user:list' },
  { to: '/system/roles', navKey: 'menu.system.role', permission: 'system:role:list' },
  { to: '/system/menu', navKey: 'menu.system.menu', permission: 'system:menu:list' },
  { to: '/system/dept', navKey: 'menu.system.dept', permission: 'system:dept:list' },
  { to: '/system/dict', navKey: 'menu.system.dict', permission: 'system:dict:list' },
  { to: '/monitor/operlog', navKey: 'menu.system.operlog', permission: 'system:operlog:list' },
  { to: '/monitor/loginlog', navKey: 'menu.system.loginlog', permission: 'system:loginlog:list' },
  { to: '/monitor/online', navKey: 'menu.system.online', permission: 'system:online:list' },
  { to: '/monitor-center/redis', navKey: 'menu.monitor.redis.info', permission: 'monitor:redis:info' },
];

/**
 * 工作台首页 —— 脚手架自带的落地页。
 * 新项目通常做两件事之一：
 *   1) 直接替换本文件内容为自己的业务概览；
 *   2) 新建业务首页并在 utils/menuMapper.ts 的 COMPONENT_MAP 中登记，
 *      再写一条迁移改掉 sys_menu 里首页那条的 component。
 */
const HomePage: React.FC = () => {
  const { t } = useTranslation('home');
  const userInfo = useUserStore((s) => s.userInfo);
  const hasPermission = useUserStore((s) => s.hasPermission);

  const links = ADMIN_LINKS.filter((l) => !l.permission || hasPermission(l.permission));

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="primary" label={t('stat.users')} fetcher={countUsers} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="info" label={t('stat.roles')} fetcher={countRoles} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="success" label={t('stat.menus')} fetcher={countMenus} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard variant="warning" label={t('stat.depts')} fetcher={countDepts} />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title={t('identity.title')}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label={t('identity.account')}>{userInfo?.username ?? '-'}</Descriptions.Item>
              <Descriptions.Item label={t('identity.name')}>{userInfo?.nickname ?? '-'}</Descriptions.Item>
              <Descriptions.Item label={t('dept', { ns: 'common' })}>{userInfo?.deptName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label={t('role', { ns: 'common' })}>
                {(userInfo?.roles ?? []).map((r) => (
                  <Tag color="blue" key={r.id}>
                    {r.roleName}（{r.roleCode}）
                  </Tag>
                ))}
              </Descriptions.Item>
              <Descriptions.Item label={t('identity.perms')}>
                <Text type="secondary">
                  {t('identity.permsCount', { count: userInfo?.permissions?.length ?? 0 })}
                </Text>
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card
            title={t('quickLinks')}
            extra={<Text type="secondary">{t('quickLinks.hint')}</Text>}
          >
            <Space size={[8, 8]} wrap>
              {links.map((l) => (
                <Link key={l.to} to={l.to}>
                  <Tag color="processing" style={{ cursor: 'pointer', margin: 0 }}>
                    {t(l.navKey, { ns: 'nav' })}
                  </Tag>
                </Link>
              ))}
            </Space>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card title={t('features')}>
            <Paragraph style={{ marginBottom: 8 }}>{t('features.summary')}</Paragraph>
            <Alert
              type="info"
              showIcon
              message={t('newModule')}
              description={
                <span>
                  {/* 整句进语言包：路径片段本身也在译文里（<c> 之间的内容由 JSON 提供），
                      这样中英文可以各自安排语序，不用把句子拆成前后缀去拼 */}
                  <Trans i18nKey="newModule.desc" ns="home" components={{ c: <Text code /> }} />
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
