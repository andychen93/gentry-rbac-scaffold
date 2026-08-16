import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);

async function getToken(page: Page): Promise<string> {
  return await page.evaluate(() => localStorage.getItem('precision_token') || '');
}

test.describe.serial('用户管理 CRUD', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
    await gotoPage(page, '/system/users');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('U-001 用户列表加载', async () => {
    await expect(page.locator('.ant-table')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.ant-table-row').first()).toBeVisible({ timeout: 5000 });
  });

  test('U-001 搜索用户（API验证）', async () => {
    // 通过 API 验证搜索功能，避免 UI 选择器问题
    const response = await page.request.get('/api/v1/users?pageNum=1&pageSize=10&username=admin', {
      headers: { 'Authorization': `Bearer ${await getToken(page)}` }
    });
    const data = await response.json();
    expect(data.code).toBe(0);
    expect(data.data.list.length).toBeGreaterThan(0);
  });

  test('U-002 新增用户', async () => {
    await page.getByRole('button', { name: '新增用户' }).click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    // 填写表单
    await page.locator('.ant-modal #username').fill('testuser' + TS);
    await page.locator('.ant-modal #nickname').fill('测试用户' + TS);
    await page.locator('.ant-modal #password').fill('Test@12345');
    await page.locator('.ant-modal #phone').fill('138' + TS + '00');

    // 提交
    await page.locator('.ant-modal').getByRole('button', { name: '确 定' }).click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
    // 弹窗应关闭
    await expect(page.locator('.ant-modal')).not.toBeVisible({ timeout: 3000 });
  });

  test('U-003 编辑用户', async () => {
    // 找到刚创建的用户行，点击编辑
    const row = page.locator('.ant-table-row').filter({ hasText: 'testuser' + TS });
    await row.getByLabel('编辑').click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    // 修改昵称
    await page.locator('.ant-modal #nickname').clear();
    await page.locator('.ant-modal #nickname').fill('已编辑用户' + TS);

    await page.locator('.ant-modal').getByRole('button', { name: '确 定' }).click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
  });

  test('U-004 重置密码', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'testuser' + TS });
    await row.getByLabel('重置密码').click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    await page.locator('.ant-modal #newPassword').fill('NewPass@123');
    // 如果有确认密码字段
    const confirmInput = page.locator('.ant-modal #confirmPassword');
    if (await confirmInput.isVisible({ timeout: 1000 }).catch(() => false)) {
      await confirmInput.fill('NewPass@123');
    }
    await page.locator('.ant-modal').getByRole('button', { name: '确 定' }).click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
  });

  test('U-006 停用账号需二次确认，取消则状态不变', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'testuser' + TS });
    const sw = row.locator('.ant-switch');
    await expect(sw).toHaveClass(/ant-switch-checked/);

    // 点开关应弹确认框，且文案里点名到具体用户
    await sw.click();
    const confirmBox = page.locator('.ant-modal-confirm');
    await expect(confirmBox).toBeVisible({ timeout: 3000 });
    await expect(confirmBox).toContainText('testuser' + TS);
    await expect(confirmBox).toContainText('无法登录');

    // 取消：Switch 受控于 status，不该自己翻过去
    await confirmBox.getByRole('button', { name: /取\s*消/ }).click();
    await expect(confirmBox).not.toBeVisible({ timeout: 3000 });
    await expect(sw).toHaveClass(/ant-switch-checked/);
  });

  test('U-006 确认后才真正停用', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'testuser' + TS });
    const sw = row.locator('.ant-switch');
    await sw.click();
    const confirmBox = page.locator('.ant-modal-confirm');
    await expect(confirmBox).toBeVisible({ timeout: 3000 });
    await confirmBox.getByRole('button', { name: /确\s*定/ }).click();

    await expect(page.locator('.ant-message').getByText('状态更新成功')).toBeVisible({ timeout: 5000 });
    await expect(row.locator('.ant-switch')).not.toHaveClass(/ant-switch-checked/, { timeout: 5000 });
  });

  test('U-007 查询/重置按钮靠右', async () => {
    /*
     * 回归守卫：查询区按钮原先跟在字段后面靠左排，视觉上和字段挤在一起。
     * 用盒模型比较「按钮右边缘」与「查询卡片内容区右边缘」，靠右时二者应基本贴合。
     */
    const search = page.getByRole('button', { name: '查询', exact: true });
    // exact：否则会同时匹配到行内的「重置密码」操作按钮
    const reset = page.getByRole('button', { name: '重置', exact: true });
    await expect(search).toBeVisible();
    await expect(reset).toBeVisible();

    // 必须锁定「包含查询按钮的那张卡」：用户页左侧还有部门树卡片，.first() 会选错
    const card = page.locator('.ant-card-body').filter({ has: reset }).first();
    const cardBox = await card.boundingBox();
    const resetBox = await reset.boundingBox();
    expect(cardBox).not.toBeNull();
    expect(resetBox).not.toBeNull();

    // 重置是最右侧那个按钮，其右边缘距卡片内容右边缘应在 padding 量级内（<40px）
    const distanceToRight = cardBox!.x + cardBox!.width - (resetBox!.x + resetBox!.width);
    expect(distanceToRight).toBeLessThan(40);

    // 且查询在重置左边
    const searchBox = await search.boundingBox();
    expect(searchBox!.x).toBeLessThan(resetBox!.x);
  });

  test('U-005 删除用户', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'testuser' + TS });
    await row.getByLabel('删除').click();
    await page.locator('.ant-popconfirm .ant-btn-primary').or(page.locator('.ant-popover .ant-btn-primary')).click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
    await expect(row).not.toBeVisible({ timeout: 3000 });
  });
});
