import { test, expect } from '@playwright/test';
import { login, gotoPage } from './helpers/auth';
import { deriveMenuKey, pack, rawKeysIn } from './helpers/i18n';

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
    expect(rawKeysIn(body), '登录页有裸 key').toEqual([]);
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

    expect(rawKeysIn(await page.locator('body').innerText()), '用户页有裸 key').toEqual([]);
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
    expect(rawKeysIn(body), '工作台有裸 key').toEqual([]);
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

  /**
   * 全站巡检。
   *
   * 每页只钉两件事：**该页 namespace 的代表性译文出现了**（说明 namespace 加载并命中）
   * 且**整页没有裸 key**（说明没有 key 拼错或漏加词条）。
   *
   * 刻意不做整页 CJK 扫描 —— 列表里全是 C 类数据（部门名、角色名、日志的 module 字面量），
   * 那些本就该是库里的中文。逐页断言译文才是能真正定位问题的形态。
   */
  const PAGE_CHECKS: { path: string; account?: string; expect: string[] }[] = [
    { path: '/system/roles', expect: ['role:table.code', 'role:action.create', 'common:status'] },
    // 树默认展开，所以按钮显示的是「折叠全部」而不是「展开全部」
    { path: '/system/dept', expect: ['dept:table.name', 'dept:action.create', 'common:collapseAll'] },
    // 菜单管理自 V15 起是 SUPER_ADMIN 专属（sys_menu 是全局表，改它影响所有租户）
    { path: '/system/menu', account: 'chenli', expect: ['menuMgmt:table.permission', 'menuMgmt:action.create'] },
    { path: '/system/dict', expect: ['dictMgmt:table.dictType', 'dictMgmt:action.createType'] },
    { path: '/system/config', expect: ['config:table.key', 'config:action.refreshCache'] },
    { path: '/monitor/operlog', expect: ['log:oper.table.id', 'log:oper.action.clean'] },
    { path: '/monitor/loginlog', expect: ['log:login.table.loginType', 'log:login.action.clean'] },
    { path: '/monitor/online', expect: ['common:loginIp', 'common:browser'] },
    { path: '/system/tenants', account: 'chenli', expect: ['tenant:table.code', 'tenant:action.create'] },
    {
      path: '/monitor-center/redis',
      account: 'chenli',
      expect: ['monitor:tab.keys', 'monitor:info.title', 'monitor:autoRefresh'],
    },
    { path: '/profile', expect: ['profile:action.changePwd', 'profile:action.edit'] },
  ];

  for (const { path, account, expect: keys } of PAGE_CHECKS) {
    test(`I18N-006 ${path} 英文渲染且无裸 key`, async ({ page }) => {
      await login(page, account ?? 'admin');
      await gotoPage(page, path);

      const body = await page.locator('body').innerText();
      for (const spec of keys) {
        const [ns, key] = spec.split(':');
        const expected = pack('en-US', ns)[key];
        expect(expected, `语言包缺 ${spec}`).toBeTruthy();
        expect(body, `${path} 未渲染 ${spec}（"${expected}"）`).toContain(expected);
      }
      expect(rawKeysIn(body), `${path} 有裸 key`).toEqual([]);
    });
  }

  /**
   * B 类 key 对账。
   *
   * **单向断言：语言包 key → 库派生 key**。反向（库里有的 key 语言包必须有）刻意不断言 ——
   * 派生项目新增菜单时不该让脚手架的测试变红，缺译文由 `makeNavLabel` 的 defaultValue
   * 回退成库里的中文，是可接受的降级。
   *
   * 这条兜的是 `MenuI18nKeyResolver` 注释里那条**隐式依赖**：`permission` 一旦成为
   * key 的来源，改动它会静默让译文退化成中文，不报错不抛异常。改 permission 而忘了改
   * `nav.json`，这里会以「孤儿 key」的形式暴露出来。
   */
  test('I18N-007 nav.json 无孤儿 key（与库里菜单派生的 key 对账）', async ({ page }) => {
    await login(page, 'chenli'); // SUPER_ADMIN 才能看到全部菜单（含租户管理）

    const tree = await page.evaluate(async () => {
      const res = await fetch('/api/v1/menus', {
        headers: { Authorization: `Bearer ${localStorage.getItem('gentry_token')}` },
      });
      return res.json();
    });
    expect(tree.code, `菜单接口失败：${JSON.stringify(tree)}`).toBe(0);

    const derived = new Set<string>();
    const walk = (nodes: { permission?: string | null; path?: string | null; children?: unknown[] }[]) => {
      for (const n of nodes) {
        const key = deriveMenuKey(n.permission, n.path);
        if (key) derived.add(key);
        if (n.children?.length) walk(n.children as typeof nodes);
      }
    };
    walk(tree.data ?? []);
    expect(derived.size, '菜单树派生出的 key 数量异常，接口结构变了？').toBeGreaterThan(30);

    for (const locale of ['zh-CN', 'en-US'] as const) {
      const orphans = Object.keys(pack(locale, 'nav')).filter((k) => !derived.has(k));
      expect(
        orphans,
        `${locale}/nav.json 里这些 key 在库里找不到对应菜单（permission/path 改过？菜单删了？）`,
      ).toEqual([]);
    }
  });

  test('I18N-008 dict.json 无孤儿 key（与库里字典派生的 key 对账）', async ({ page }) => {
    await login(page, 'chenli');

    const derived = await page.evaluate(async () => {
      const auth = { Authorization: `Bearer ${localStorage.getItem('gentry_token')}` };
      const types = await (
        await fetch('/api/v1/dict/types?pageNum=1&pageSize=200', { headers: auth })
      ).json();
      const keys: string[] = [];
      for (const t of types.data?.list ?? []) {
        keys.push(`dict.type.${t.dictType}`);
        const data = await (
          await fetch(`/api/v1/dict/types/${t.dictType}/data`, { headers: auth })
        ).json();
        for (const d of data.data ?? []) keys.push(`dict.${t.dictType}.${d.dictValue}`);
      }
      return keys;
    });
    const set = new Set(derived);
    expect(set.size, '字典派生出的 key 数量异常').toBeGreaterThan(15);

    for (const locale of ['zh-CN', 'en-US'] as const) {
      const orphans = Object.keys(pack(locale, 'dict')).filter((k) => !set.has(k));
      expect(
        orphans,
        `${locale}/dict.json 里这些 key 在库里找不到对应字典项（dict_type/dict_value 改过？项删了？）`,
      ).toEqual([]);
    }
  });

  /**
   * 职务下拉是「清理业务域残留」那批改动的可见成果。
   *
   * 在 V13 之前 `sys_user.post_name` 存的是中文 label（「经理」），下拉的 value 必须与库里
   * 一致，所以**英文界面下这个下拉只能是中文**，前端还得为此维护一份 `POST_NAMES` 中文常量、
   * 并给 i18n 防线开一个豁免。改成存 `sys_user_post` 的字典码之后文案才能翻。
   */
  test('I18N-009 职务下拉是英文（post_name 存字典码的可见成果）', async ({ page }) => {
    await login(page);
    await gotoPage(page, '/system/users');
    await page.getByRole('button', { name: userEn['action.create'] }).click();
    await expect(page.getByText(userEn['form.title.create'])).toBeVisible();

    const modal = page.locator('.ant-modal-content');
    await modal.locator('#postName').click();

    const dictEn = pack('en-US', 'dict');
    const dropdown = page.locator('.ant-select-dropdown:visible');
    // 「Chief Architect」带空格、与码 ChiefArchitect 不同，最能说明显示的是译文而非码
    await expect(dropdown.getByText(dictEn['dict.sys_user_post.ChiefArchitect'], { exact: true }))
      .toBeVisible();
    await expect(dropdown.getByText(dictEn['dict.sys_user_post.Supervisor'], { exact: true }))
      .toBeVisible();
    // 「司机 / Driver」已由 V13 从字典里删掉
    await expect(dropdown.getByText('Driver', { exact: true })).toHaveCount(0);
    await expect(dropdown.getByText('主管', { exact: true })).toHaveCount(0);
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
