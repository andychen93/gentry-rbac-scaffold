import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

/**
 * 操作日志 / 登录日志的「下拉 table」筛选（PageSelect）。
 *
 * PageSelect 组件一直存在，但此前只有 /dev/style 预览页在用，
 * 没有任何业务页面覆盖 —— 这组用例把它钉在真实筛选链路上。
 */
test.describe.serial('日志筛选：下拉 table (LOG-F)', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page, 'chenli');
    await gotoPage(page, '/monitor/operlog');
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('LOG-F01 日志编号不换行（列够宽）', async () => {
    await expect(page.locator('.ant-table').first()).toBeVisible({ timeout: 10000 });
    const cell = page.locator('.ant-table-tbody .ant-table-row').first().locator('td').first();
    await expect(cell).toBeVisible();

    // 直接数文本渲染成几行：用 Range 的 client rects，比拿高度猜阈值准
    const lineCount = await cell.evaluate((el) => {
      const range = document.createRange();
      range.selectNodeContents(el);
      return range.getClientRects().length;
    });
    expect(lineCount).toBe(1);
    // 18 位雪花 ID 应完整显示
    await expect(cell).toHaveText(/^\d{15,20}$/);
  });

  test('LOG-F02 操作用户是下拉 table，点开有用户表格', async () => {
    // 该筛选框是只读 Input，点击弹出 Popover 里的分页表格
    const input = page.locator('.ant-card').first().getByPlaceholder('点击选择用户');
    await expect(input).toBeVisible();
    await input.click();

    const pop = page.locator('.ant-popover:visible');
    await expect(pop).toBeVisible({ timeout: 5000 });
    // 表头应是用户列，而不是普通下拉的一列选项
    await expect(pop.getByRole('columnheader', { name: '用户名' })).toBeVisible();
    await expect(pop.getByRole('columnheader', { name: '昵称' })).toBeVisible();
    await expect(pop.getByRole('columnheader', { name: '部门' })).toBeVisible();
    await expect(pop.locator('.ant-table-row').first()).toBeVisible({ timeout: 5000 });
  });

  test('LOG-F03 选中用户后回填并能筛出该用户的日志', async () => {
    const pop = page.locator('.ant-popover:visible');
    // 选 chenli 这一行（种子账号，必然有操作日志）
    await pop.locator('.ant-table-row').filter({ hasText: 'chenli' }).first().click();
    await expect(pop).toBeHidden({ timeout: 5000 });

    const input = page.locator('.ant-card').first().getByPlaceholder('点击选择用户');
    await expect(input).toHaveValue('chenli');

    await page.getByRole('button', { name: '查询', exact: true }).click();

    /*
     * 必须锚定「日志表」本身：下拉 table 关闭后 antd 仍把 Popover 里的用户表格
     * 留在 DOM 里，裸用 .ant-table-tbody 会把那张表的行也算进来。
     */
    const logTable = page.locator('.ant-table').filter({
      has: page.getByRole('columnheader', { name: '日志编号' }),
    });
    await expect(logTable.locator('tbody .ant-table-row').first()).toBeVisible({ timeout: 10000 });
    // 结果里的「操作用户」列应全是 chenli
    const operators = logTable.locator('tbody .ant-table-row td:nth-child(2)');
    const n = await operators.count();
    expect(n).toBeGreaterThan(0);
    for (let i = 0; i < n; i++) {
      await expect(operators.nth(i)).toHaveText('chenli');
    }
  });

  test('LOG-F04 可清除已选用户（查询条件要能取消）', async () => {
    const input = page.locator('.ant-card').first().getByPlaceholder('点击选择用户');
    await expect(input).toHaveValue('chenli');
    // 自绘的清除图标（antd 自带 allowClear 在 readOnly 输入框上会被隐藏）
    await page.locator('.ant-card').first().locator('.ps-page-select-clear').first().click();
    await expect(input).toHaveValue('');
  });

  test('LOG-F05 模块是下拉 table，模糊查菜单表', async () => {
    const input = page.locator('.ant-card').first().getByPlaceholder('点击选择模块');
    await expect(input).toBeVisible();
    await input.click();

    const pop = page.locator('.ant-popover:visible');
    await expect(pop).toBeVisible({ timeout: 5000 });
    await expect(pop.getByRole('columnheader', { name: '位置' })).toBeVisible();  // 菜单层级路径列

    // 模糊搜索「角色」应命中角色管理菜单
    await pop.getByPlaceholder('输入关键字搜索...').fill('角色');
    await pop.getByPlaceholder('输入关键字搜索...').press('Enter');
    await expect(pop.locator('.ant-table-row').filter({ hasText: '角色管理' }).first())
      .toBeVisible({ timeout: 5000 });

    // 选中后回填菜单名
    await pop.locator('.ant-table-row').filter({ hasText: '角色管理' }).first().click();
    await expect(pop).toBeHidden({ timeout: 5000 });
    await expect(input).toHaveValue('角色管理');
  });

  test('LOG-F06 登录日志的用户名也是下拉 table', async () => {
    await gotoPage(page, '/monitor/loginlog');
    const input = page.locator('.ant-card').first().getByPlaceholder('点击选择用户');
    await expect(input).toBeVisible();
    await input.click();
    const pop = page.locator('.ant-popover:visible');
    await expect(pop).toBeVisible({ timeout: 5000 });
    await expect(pop.locator('.ant-table-row').first()).toBeVisible({ timeout: 5000 });
  });
});
