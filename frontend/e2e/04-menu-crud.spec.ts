import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';

const TS = Date.now().toString().slice(-6);

// 菜单管理是 SUPER_ADMIN 专属，必须用 chenli 登录。
// sys_menu 是全局表（菜单不做多租户），一份菜单树被所有租户共用，改它是平台动作 ——
// V15 把 system:menu 与 {add,edit,remove} 标成 is_platform=1，ADMIN 连侧边栏都看不到这页。
// 只有 system:menu:list 留给了 ADMIN：「角色 → 权限」页要靠它拉菜单树画勾选框。
test.describe.serial('菜单管理 CRUD', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page, 'chenli');
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
    // 强断言而非 if 包裹：M-002 已在同一 serial describe 里创建了这条菜单，
    // 找不到就是真出问题。原先用 if 静默跳过，长期掩盖了下面那个确认按钮定位错误
    const row = page.locator('.ant-table-row').filter({ hasText: '测试按钮' + TS });
    await expect(row).toBeVisible({ timeout: 5000 });

    await row.getByLabel('删除').click();
    // 与其余删除用例保持一致：按 class 定位 Popconfirm 的确认按钮。
    // 不用 getByRole('button',{name:'确定'})——antd 会在两个中文字之间插空格（"确 定"），匹配不到
    await page.locator('.ant-popconfirm .ant-btn-primary').or(page.locator('.ant-popover .ant-btn-primary')).click();
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    // 确认真的从列表里消失了（原用例只校验有提示，删没删掉并不验证）
    await expect(row).toHaveCount(0, { timeout: 5000 });
  });
});
