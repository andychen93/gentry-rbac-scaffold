import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);

test.describe.serial('菜单管理 CRUD', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
    await gotoPage(page, '/system/menu');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('M-001 菜单树加载', async () => {
    await expect(page.locator('.ant-table')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('系统管理').first()).toBeVisible();
  });

  test('M-002 新增按钮菜单', async () => {
    // 限定工具栏：行内操作图标也是 role=button（aria-label「新增下级」），
    // 用宽松的 /新增/ 会同时匹配到行内图标而报 strict mode 冲突
    await page.locator('.ant-card').getByRole('button', { name: '新增菜单' }).click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    // 选择类型=按钮
    await page.locator('.ant-modal').getByText('按钮').click();
    await page.locator('.ant-modal #name').fill('测试按钮' + TS);
    await page.locator('.ant-modal #permission').fill('test:btn:' + TS);
    // 排序
    const sortInput = page.locator('.ant-modal #sort');
    if (await sortInput.isVisible()) {
      await sortInput.clear();
      await sortInput.fill('99');
    }

    await page.locator('.ant-modal .ant-btn-primary').click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await page.waitForTimeout(1000);
  });

  test('M-004 删除菜单', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: '测试按钮' + TS });
    if (await row.isVisible({ timeout: 3000 }).catch(() => false)) {
      await row.getByLabel('删除').click();
      await page.getByRole('button', { name: '确定' }).click();
      await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
      await page.waitForTimeout(1000);
    }
  });
});
