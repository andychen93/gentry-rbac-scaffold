import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

async function gotoRedisMonitor(page: Page) {
  await gotoPage(page, '/monitor-center/redis');
}

test.describe('Redis 监控页面（MON-REDIS-001 ~ MON-REDIS-007）', () => {
  test('MON-REDIS-001 监控信息 Tab 加载', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    await gotoRedisMonitor(page);

    await expect(page.getByRole('heading', { name: 'Redis 监控' })).toBeVisible({
      timeout: 10000,
    });
    await expect(page.getByText('服务基本信息')).toBeVisible();
    await expect(page.getByText('内存使用率').first()).toBeVisible();
    await expect(page.getByText('Key 定义')).toBeVisible();
    await expect(page.getByText('jwt_blacklist').first()).toBeVisible();
  });

  test('MON-REDIS-002 Key 管理 Tab 搜索与表格', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    await gotoRedisMonitor(page);

    await page.getByRole('tab', { name: 'Key 管理' }).click();
    const input = page.getByPlaceholder(/Key 模式/);
    await expect(input).toBeVisible();
    await input.fill('Authorization:*');
    await page.getByRole('button', { name: '搜索' }).click();
    await page.waitForLoadState('networkidle');
    // 当前 session 至少会有 admin 自己，断言结果区出现 Authorization:login:token: 前缀
    await expect(
      page.locator('.ant-tabs-tabpane-active').getByText(/Authorization:login:(token|session):/).first()
    ).toBeVisible({ timeout: 10000 });
  });

  test('MON-REDIS-003 Key 定义快捷查询跳转 Key 管理 Tab', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    await gotoRedisMonitor(page);

    await page
      .getByRole('row', { name: /jwt_blacklist/ })
      .getByRole('button', { name: '查询 Key' })
      .click();

    const input = page.getByPlaceholder(/Key 模式/);
    await expect(input).toHaveValue('blacklist:*');
  });

  test('MON-REDIS-004 ADMIN 可以删除 Key（去多租户化后 ADMIN 是唯一角色，持有全部权限）', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    await gotoRedisMonitor(page);

    await page.getByRole('tab', { name: 'Key 管理' }).click();
    await page.getByPlaceholder(/Key 模式/).fill('Authorization:*');
    await page.getByRole('button', { name: '搜索' }).click();
    await page.waitForLoadState('networkidle');

    // 等待当前 Tab 面板内出现搜索结果
    const activePane = page.locator('.ant-tabs-tabpane-active');
    await expect(
      activePane.getByText(/Authorization:login:(token|session):/).first()
    ).toBeVisible({ timeout: 10000 });

    // 操作列已改为纯图标 + Tooltip，按 aria-label 定位（不再是带文字的 button）
    // ADMIN 持有 monitor:redis:key:delete，"删除"与"查看"均应存在
    expect(await activePane.getByLabel('删除').count()).toBeGreaterThan(0);
    expect(await activePane.getByLabel('查看').count()).toBeGreaterThan(0);
  });

  test('MON-REDIS-005 慢查询日志 Tab 加载', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    await gotoRedisMonitor(page);

    await page.getByRole('tab', { name: '慢查询日志' }).click();
    await page.waitForLoadState('networkidle');

    // 刷新按钮可见
    await expect(page.getByRole('button', { name: /刷新/ }).last()).toBeVisible();
    // ADMIN 持有 monitor:redis:slowlog:reset，可见"清空慢日志"按钮
    await expect(page.getByRole('button', { name: '清空慢日志' })).toBeVisible();
  });

  test('MON-REDIS-007 自动刷新开关切换', async ({ page }) => {
    await login(page, 'admin', 'Abc@123456');
    await gotoRedisMonitor(page);

    // 页面工具栏内的 Switch（"自动刷新" 文本旁）
    const switcher = page.locator('.ant-switch').first();
    await expect(switcher).toBeVisible();
    const checkedBefore = await switcher.getAttribute('aria-checked');
    await switcher.click();
    await expect
      .poll(async () => await switcher.getAttribute('aria-checked'))
      .not.toBe(checkedBefore);
  });
});
