import { test, expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import fs from 'node:fs';
import { TOKENS_FILE } from './global-setup';

/**
 * 平台级权限隔离（ISO）—— 租户管理员越权的端到端回归。
 *
 * <b>缺陷现场</b>：新建租户的 admin 能列出/新增/修改/删除**所有**租户，包括默认租户。
 *
 * <b>为什么既有测试没抓到</b>：`T-003 ADMIN 无租户管理权限` 测的是**种子** admin
 * （`role_id=1`，权限基线正确），而「建租户」那条路径给出的权限完全不同 ——
 * 两条路径结果不一致，测试正好只覆盖了对的那条。所以本文件的用例必须**自己建一个租户**。
 *
 * 后端 `PlatformPermissionIsolationApiIT` 已覆盖同一批性质（且验证过会红）。
 * 这里补的是它覆盖不到的两件事：
 *   1. 真实 HTTP 全链路（IT 跑在会回滚的事务里）
 *   2. 前端渲染 —— 侧边栏与权限分配树是否真的看不到平台级权限点
 */

/**
 * 每次跑都建一个**新**租户，用完删掉。
 *
 * 为什么不复用固定租户：建租户返回的 admin 初始密码只显示一次，重跑时那个密码已经丢了；
 * 而「让平台超管把它重置成已知值」这条路走不通 —— 实测
 * {@code GET /api/v1/users} 对平台超管**仍按租户过滤**（只返回默认租户的 3 个用户），
 * 所以拿不到别的租户的用户 ID。
 * （`TenantConstants` 的注释写着「平台超管跨租户可见所有数据」，与实现不一致，已记为待办。）
 */
const TENANT_PREFIX = 'e2eiso';
const TENANT_NAME = 'E2E 隔离验证租户';

interface IsolatedTenant {
  id: string;
  code: string;
  token: string;
}

function superToken(): string {
  const tokens = JSON.parse(fs.readFileSync(TOKENS_FILE, 'utf-8')) as Record<string, string>;
  const t = tokens['chenli'];
  if (!t) throw new Error('global-setup 未预登录 chenli');
  return t;
}

/** 建租户并用它的 admin 登录，返回租户 id / code / token */
async function createIsolatedTenant(request: APIRequestContext): Promise<IsolatedTenant> {
  const auth = { Authorization: `Bearer ${superToken()}` };
  // 6-20 位字母数字；用时间戳后 6 位保证多次跑不撞
  const code = `${TENANT_PREFIX}${String(Date.now()).slice(-6)}`;

  const created = await request.post('/api/v1/tenants', {
    headers: auth,
    data: { code, name: TENANT_NAME, contact: 'E2E' },
  });
  const body = await created.json();
  expect(body.code, `建租户失败：${JSON.stringify(body)}`).toBe(0);

  const login = await request.post('/api/v1/auth/login', {
    data: { tenantCode: code, username: body.data.adminUsername, password: body.data.adminPassword },
  });
  const loginBody = await login.json();
  expect(loginBody.code, `新租户 admin 登录失败：${JSON.stringify(loginBody)}`).toBe(0);

  return { id: String(body.data.tenantId), code, token: loginBody.data.token };
}

/**
 * 逻辑删除该租户。
 *
 * E2E 不像 IT 那样跑在会回滚的事务里，不清理就会一直堆租户。
 * 删除接口要求租户下只剩 admin 一个用户，本用例满足。
 * 已知残留：该租户的 user / role / dept 行仍在库里，但它们归属的租户已被删，
 * 任何界面都看不到 —— 对开发库可接受。
 */
async function removeTenant(request: APIRequestContext, tenantId: string): Promise<void> {
  const res = await request.delete(`/api/v1/tenants/${tenantId}`, {
    headers: { Authorization: `Bearer ${superToken()}` },
  });
  const body = await res.json().catch(() => ({}));
  if (body.code !== 0) {
    // 清理失败不该让用例红，但要留下线索
    console.warn(`[ISO] 清理租户 ${tenantId} 失败：${JSON.stringify(body)}`);
  }
}

test.describe.serial('平台级权限隔离 (ISO)', () => {
  let iso: IsolatedTenant;
  let isoToken: string;

  test.beforeAll(async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: 'http://localhost:9090' });
    iso = await createIsolatedTenant(request);
    isoToken = iso.token;
    await request.dispose();
  });

  test.afterAll(async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: 'http://localhost:9090' });
    await removeTenant(request, iso.id);
    await request.dispose();
  });

  test('ISO-001 新租户 admin 调租户接口返回 403（原缺陷是 200）', async ({ playwright }) => {
    const request = await playwright.request.newContext({
      baseURL: 'http://localhost:9090',
      extraHTTPHeaders: { Authorization: `Bearer ${isoToken}` },
    });
    // 修复前这里是 200，且返回体里能看到默认租户
    expect((await request.get('/api/v1/tenants?pageNum=1&pageSize=10')).status()).toBe(403);
    expect((await request.get('/api/v1/tenants/1')).status()).toBe(403);
    await request.dispose();
  });

  test('ISO-002 新租户 admin 的权限里没有平台级权限点', async ({ playwright }) => {
    const request = await playwright.request.newContext({
      baseURL: 'http://localhost:9090',
      extraHTTPHeaders: { Authorization: `Bearer ${isoToken}` },
    });
    const info = (await (await request.get('/api/v1/auth/user-info')).json()).data;
    const perms: string[] = info.permissions ?? [];

    expect(perms.length, '不该是空角色 —— 它是租户下的最高权限').toBeGreaterThan(10);
    expect(perms.filter((p) => p.startsWith('system:tenant'))).toEqual([]);
    expect(perms).not.toContain('monitor:redis:key:delete');
    expect(perms).not.toContain('monitor:redis:slowlog:reset');
    // 只读的仍保留：租户管理员可以看共享基础设施，不能改它
    expect(perms).toContain('monitor:redis:info');
    // 平台超管标记由后端下发，前端据此隐藏权限分配树里的平台级节点
    expect(info.platformAdmin).toBe(false);
    await request.dispose();
  });

  test('ISO-003 新租户 admin 无法自己把平台级权限勾给角色（自提权路径）', async ({ playwright }) => {
    const request = await playwright.request.newContext({
      baseURL: 'http://localhost:9090',
      extraHTTPHeaders: { Authorization: `Bearer ${isoToken}` },
    });
    /*
     * 这条是「只修新建租户的基线是安全剧场」的证据：租户管理员握有
     * system:role:assignMenu，即使新建时没给它租户管理权限，它也能自己勾回来。
     */
    const roleRes = await request.post('/api/v1/roles', {
      data: { roleCode: `iso${Date.now() % 100000}`, roleName: '提权尝试', sort: 1 },
    });
    const roleBody = await roleRes.json();
    expect(roleBody.code, `建角色失败：${JSON.stringify(roleBody)}`).toBe(0);

    // 101 / 1011 是「租户管理」目录与其查询按钮，V14 已标为 is_platform = 1
    const assign = await request.put(`/api/v1/roles/${roleBody.data.id}/menus`, {
      data: { menuIds: [101, 1011] },
    });
    const assignBody = await assign.json();
    expect(assignBody.code, '应被 PLATFORM_MENU_FORBIDDEN 拒绝').toBe(40004);
    expect(assignBody.message, '报错要本地化，不能露出裸 key').not.toContain('error.platform');

    // 清理：删掉这个尝试用的角色，避免重复跑堆积
    await request.delete(`/api/v1/roles/${roleBody.data.id}`);
    await request.dispose();
  });

  test('ISO-004 新租户 admin 的侧边栏看不到租户管理', async ({ page }) => {
    await page.addInitScript(
      ([key, value]) => window.localStorage.setItem(key, value),
      ['gentry_token', isoToken] as const,
    );
    await page.goto('/');
    await expect(page.getByTestId('session-restore')).toHaveCount(0, { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // 侧边栏由后端下发的菜单树驱动，没权限的菜单根本不会下发
    await expect(page.locator('.ps-sidenav').getByText('租户管理')).toHaveCount(0);
    // 直接敲地址也进不去（页面级 AccessDenied / 重定向），至少不能看到租户数据
    await page.goto('/system/tenants');
    await expect(page.getByTestId('session-restore')).toHaveCount(0, { timeout: 15000 });
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('默认租户')).toHaveCount(0);
  });

  test('ISO-005 权限分配树对租户管理员隐藏平台级节点', async ({ page }) => {
    await page.addInitScript(
      ([key, value]) => window.localStorage.setItem(key, value),
      ['gentry_token', isoToken] as const,
    );
    await page.goto('/system/roles');
    await expect(page.getByTestId('session-restore')).toHaveCount(0, { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // 进它自己 ADMIN 角色的权限页
    await page.locator('.ant-table-tbody .ant-table-row').first().waitFor({ timeout: 10000 });
    await page.locator('.ant-table-tbody .ant-table-row').first()
      .getByRole('button', { name: /权限/ }).click();
    await expect(page.getByText('菜单权限')).toBeVisible({ timeout: 10000 });

    /*
     * 隐藏是**体验层**措施（真正的守卫在后端 assignMenus）：让人不去点一个必然被拒的勾选框。
     * 但它同时也是 isPlatform 字段确实下发到前端的证据 —— 字段缺失会静默退化成「全都显示」。
     */
    await expect(page.locator('.ant-tree').getByText('租户管理')).toHaveCount(0);
    // Redis 监控本身是租户级（可看），只有两个破坏性按钮是平台级
    await expect(page.locator('.ant-tree').getByText('Key 删除')).toHaveCount(0);
    await expect(page.locator('.ant-tree').getByText('慢日志清空')).toHaveCount(0);
    // V15：菜单管理也是平台级（sys_menu 是全局表，一份菜单树所有租户共用）
    await expect(page.locator('.ant-tree').getByText('菜单管理')).toHaveCount(0);
  });

  test('ISO-006 平台超管仍看得到平台级节点（守卫不过度拦截）', async ({ page }) => {
    const tokens = JSON.parse(fs.readFileSync(TOKENS_FILE, 'utf-8')) as Record<string, string>;
    await page.addInitScript(
      ([key, value]) => window.localStorage.setItem(key, value),
      ['gentry_token', tokens['chenli']] as const,
    );
    await page.goto('/system/roles');
    await expect(page.getByTestId('session-restore')).toHaveCount(0, { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    await page.locator('.ant-table-tbody .ant-table-row').first().waitFor({ timeout: 10000 });
    await page.locator('.ant-table-tbody .ant-table-row').first()
      .getByRole('button', { name: /权限/ }).click();
    await expect(page.getByText('菜单权限')).toBeVisible({ timeout: 10000 });

    /*
     * 这三条同时是 ISO-005 的**对照**：ISO-005 断言的是 `toHaveCount(0)`，
     * 文案写错也会通过。这里确认同样的选择器 + 文案在超管树里确实能定位到，
     * ISO-005 的 0 才是「被隐藏了」而不是「一直找不到」。
     */
    await expect(page.locator('.ant-tree').getByText('租户管理')).toHaveCount(1);
    await expect(page.locator('.ant-tree').getByText('Key 删除')).toHaveCount(1);
    await expect(page.locator('.ant-tree').getByText('慢日志清空')).toHaveCount(1);
    await expect(page.locator('.ant-tree').getByText('菜单管理')).toHaveCount(1);
  });
});
