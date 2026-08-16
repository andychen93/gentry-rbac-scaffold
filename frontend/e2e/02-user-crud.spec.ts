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

  test('U-005 删除用户', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: 'testuser' + TS });
    await row.getByLabel('删除').click();
    await page.locator('.ant-popconfirm .ant-btn-primary').or(page.locator('.ant-popover .ant-btn-primary')).click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
    await expect(row).not.toBeVisible({ timeout: 3000 });
  });
});
