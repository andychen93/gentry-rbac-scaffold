import { test, expect } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';
import { pack } from './helpers/i18n';

/**
 * 国际化验收（I18N）。
 *
 * 全套用例强制英文界面（`test.use({ locale: 'en-US' })` + 预置 localStorage），
 * 与其余 spec 的中文断言互补 —— 其余 spec 由 playwright.config 固定成 zh-CN。
 *
 * **为什么两个来源都要设**：`resolveInitialLocale()` 的优先级是
 * localStorage → navigator.language → 默认。只设浏览器 locale 时，上一条用例
 * 若写过 localStorage 就会串味；只设 localStorage 则 antd/dayjs 之外的浏览器行为
 * （日期格式、输入法）仍是中文环境。
 */
test.use({ locale: 'en-US' });

const commonEn = pack('en-US', 'common');
const userEn = pack('en-US', 'user');
const homeEn = pack('en-US', 'home');
const loginEn = pack('en-US', 'login');

/**
 * 裸 key 特征：译文里不会出现 `action.create` 这种点分小写标识符。
 * 命中说明 key 没在语言包里（t() 缺 key 时原样返回 key）。
 */
const RAW_KEY =
  /\b(action|form|table|msg|pwd|stat|identity|quickLinks|newModule|tab|captcha|tenant|placeholder|valid|confirm|roleAssign|app)\.[a-zA-Z][a-zA-Z0-9.]*/;

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => window.localStorage.setItem('gentry_locale', 'en-US'));
});

test.describe('国际化 (I18N)', () => {
  test('I18N-001 登录页全英文、无裸 key', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(commonEn['app.name'])).toBeVisible();
    await expect(page.getByText(loginEn['subtitle'])).toBeVisible();
    await expect(page.getByRole('button', { name: loginEn['submit'] })).toBeVisible();
    await page.getByRole('tab', { name: loginEn['tab.tenant'] }).click();
    await expect(page.getByText(loginEn['defaultTenant'])).toBeVisible();

    // 登录页整页没有 C 类数据（租户名除外，默认部署下只有内置租户），可以全页扫中文
    const body = await page.locator('body').innerText();
    expect(/[\u4e00-\u9fff]/.test(body), `登录页有中文残留：\n${body}`).toBe(false);
    expect(RAW_KEY.test(body), `登录页有裸 key：\n${body}`).toBe(false);
  });

  test('I18N-002 用户管理页表头与工具栏英文', async ({ page }) => {
    await login(page);
    await gotoPage(page, '/system/users');

    for (const label of [
      commonEn['username'],
      commonEn['nickname'],
      commonEn['dept'],
      userEn['table.roles'],
      commonEn['status'],
      userEn['table.action'],
    ]) {
      await expect(page.locator('th').filter({ hasText: label }).first()).toBeVisible();
    }
    await expect(page.getByRole('button', { name: userEn['action.create'] })).toBeVisible();

    const body = await page.locator('body').innerText();
    expect(RAW_KEY.test(body), `用户页有裸 key：\n${body}`).toBe(false);
  });

  test('I18N-003 前端校验提示与后端错误码都随 Accept-Language 走英文', async ({ page }) => {
    await login(page);
    await gotoPage(page, '/system/users');
    await page.getByRole('button', { name: userEn['action.create'] }).click();
    await expect(page.getByText(userEn['form.title.create'])).toBeVisible();

    // ① 前端 rules（Form.Item 的 message）
    await page.getByRole('button', { name: 'OK' }).click();
    await expect(page.getByText(commonEn['placeholder.username'])).toBeVisible();
    await expect(page.getByText(commonEn['placeholder.nickname'])).toBeVisible();
    await expect(page.getByText(commonEn['placeholder.password'])).toBeVisible();

    /*
     * ② 后端 ErrorCode。用 zhangsan 而不是 admin —— admin 是保留用户名，
     * 会先撞「保留用户名」而不是「已存在」，测不到本条想测的分支。
     */
    const modal = page.locator('.ant-modal-content');
    await modal.locator('#username').fill('zhangsan'); // 查询表单也有 #username，必须限定弹窗内
    await modal.locator('#nickname').fill('probe');
    await modal.locator('#password').fill('Abc@123456');

    const pending = page.waitForResponse(
      (r) => r.url().includes('/api/v1/users') && r.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'OK' }).click();
    const resp = await pending;
    expect(resp.request().headers()['accept-language']).toBe('en-US');
    expect((await resp.json()).message).toBe('Username already exists');
    await expect(
      page.locator('.ant-message').getByText('Username already exists'),
    ).toBeVisible({ timeout: 10000 });
  });

  test('I18N-004 工作台英文，快捷入口走 nav 派生 key', async ({ page }) => {
    await login(page);
    await gotoPage(page, '/home');

    await expect(page.getByText(homeEn['identity.title'])).toBeVisible();
    await expect(page.getByText(homeEn['quickLinks'])).toBeVisible();
    await expect(page.getByText(homeEn['quickLinks.hint'])).toBeVisible();
    await expect(page.getByText(homeEn['features'])).toBeVisible();
    await expect(page.getByText(homeEn['newModule'])).toBeVisible();
    // 快捷入口标签取 nav namespace 的派生 key，与后端 MenuI18nKeyResolver 同规则
    await expect(page.getByText(pack('en-US', 'nav')['menu.system.user']).first()).toBeVisible();

    const body = await page.locator('body').innerText();
    expect(RAW_KEY.test(body), `工作台有裸 key：\n${body}`).toBe(false);
    /*
     * 这里**不做整页中文扫描**：页面上剩的中文是 C 类数据（nickname「管理员」、
     * deptName「总公司」、roleName「管理员」）和一个真实文件名，都是库里的值，
     * 不该被 i18n 拦。只钉「原先硬编码的那批文案确实不见了」。
     */
    for (const gone of [
      '当前登录身份',
      '权限点',
      '系统管理快捷入口',
      '顶栏齿轮图标可切换到系统管理布局',
      '脚手架自带能力',
      '开始一个新模块',
      '用户数',
      '部门数',
    ]) {
      expect(body, `工作台还有硬编码中文「${gone}」`).not.toContain(gone);
    }
  });

  test('I18N-005 顶栏切语言：界面与侧边栏立即变，且不重拉菜单', async ({ page }) => {
    /*
     * 用 zhangsan 而不是 admin：切语言会把偏好落进 `sys_user.language`，
     * 而三级链里用户偏好优先级最高 —— 拿 admin 切完，I18N-002~004 那几条
     * 「localStorage 说英文」的用例下次跑就会被库里的偏好按回中文。
     *
     * 用例最后切回英文，让 zhangsan.language 稳定在 en_US，重复跑结果一致。
     */
    const userZh = pack('zh-CN', 'user');
    const navZh = pack('zh-CN', 'nav');

    await login(page, 'zhangsan');
    await gotoPage(page, '/system/users');
    await expect(page.getByRole('button', { name: userEn['action.create'] })).toBeVisible();

    const switcher = page.getByTestId('locale-switcher');
    const pickLanguage = async (nativeName: string) => {
      await switcher.click();
      await page.getByRole('menuitem', { name: nativeName }).click();
    };

    /*
     * 切中文：菜单树不该被重新拉取。菜单随 `/api/v1/auth/user-info` 一起下发，
     * store 里存的是原始 `{ name, i18nKey }`，label 在渲染时才算 —— 这正是
     * 「后端发 key、前端翻」相对「后端发译好的成品」的收益，所以要钉住。
     */
    let userInfoCalls = 0;
    page.on('request', (r) => {
      if (r.url().includes('/api/v1/auth/user-info')) userInfoCalls += 1;
    });
    await pickLanguage('简体中文');

    await expect(page.getByRole('button', { name: userZh['action.create'] })).toBeVisible({
      timeout: 10000,
    });
    await expect(page.locator('.ps-sidenav').getByText(navZh['menu.system']).first()).toBeVisible();
    expect(userInfoCalls, '切语言不该重新请求 user-info（菜单树）').toBe(0);

    // 切回英文，恢复账号状态
    await pickLanguage('English');
    await expect(page.getByRole('button', { name: userEn['action.create'] })).toBeVisible({
      timeout: 10000,
    });
  });
});
