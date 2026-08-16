import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);
const KEY = `test.e2e.key${TS}`;

test.describe.serial('参数配置 (CFG-001 ~ CFG-005)', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page, 'chenli');
    await gotoPage(page, '/system/config');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('CFG-001 参数列表加载', async () => {
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
    // 种子参数（common/V2__init_data.sql）
    await expect(page.getByText('sys.login.maxFailCount')).toBeVisible();
  });

  test('CFG-002 工具栏两个按钮之间有间距', async () => {
    /*
     * 回归守卫：ProTable 的 toolbar 只是塞进一个普通 div，不会自动加 gap。
     * 曾经这里用裸 <> 包两个按钮，导致「新增参数」和「刷新缓存」完全贴在一起。
     * 用真实盒模型算水平间隙，比断言 class 名可靠。
     */
    const add = page.getByRole('button', { name: '新增参数' });
    const refresh = page.getByRole('button', { name: '刷新缓存' });
    await expect(add).toBeVisible();
    await expect(refresh).toBeVisible();

    const a = await add.boundingBox();
    const r = await refresh.boundingBox();
    expect(a).not.toBeNull();
    expect(r).not.toBeNull();
    // 两个按钮同一行，且中间留有空隙（antd Space 默认 small = 8px）
    expect(Math.abs(a!.y - r!.y)).toBeLessThan(4);
    const gap = r!.x - (a!.x + a!.width);
    expect(gap).toBeGreaterThanOrEqual(4);
  });

  test('CFG-003 新增参数', async () => {
    await page.getByRole('button', { name: '新增参数' }).click();
    await expect(page.locator('.ant-modal')).toBeVisible({ timeout: 3000 });

    await page.locator('.ant-modal #configName').fill('E2E 测试参数' + TS);
    await page.locator('.ant-modal #configKey').fill(KEY);
    await page.locator('.ant-modal #configValue').fill('v1');

    await page.locator('.ant-modal .ant-btn-primary').click();
    await expect(page.locator('.ant-message').getByText('新增成功')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(KEY)).toBeVisible({ timeout: 5000 });
  });

  test('CFG-004 刷新缓存', async () => {
    await page.getByRole('button', { name: '刷新缓存' }).click();
    await expect(page.locator('.ant-message').getByText('缓存已刷新')).toBeVisible({ timeout: 5000 });
  });

  test('CFG-005 删除参数', async () => {
    const row = page.locator('.ant-table-row').filter({ hasText: KEY });
    await expect(row).toBeVisible({ timeout: 5000 });
    await row.getByLabel('删除').click();
    await page.locator('.ant-popconfirm .ant-btn-primary').click();
    await expect(page.locator('.ant-message').getByText('删除成功')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(KEY)).toHaveCount(0, { timeout: 5000 });
  });
});
