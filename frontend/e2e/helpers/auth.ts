import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import fs from 'node:fs';
import { TOKENS_FILE, readCaptchaAnswer } from '../global-setup';

const TOKEN_KEY = 'gentry_token';

/**
 * 打开登录页，并在开启了验证码时取回正确答案。
 *
 * 验证码 uuid 只存在页面的 React state 里（DOM 读不到），所以拦截页面自己发的
 * /auth/captcha 响应拿 uuid，再从 Redis 读答案。关闭验证码时返回 null。
 */
export async function gotoLoginAndGetCaptcha(page: Page): Promise<string | null> {
  /*
   * 收集本次导航期间所有验证码响应，取「最后一个」。
   *
   * main.tsx 开了 React.StrictMode，开发模式下 effect 会双调用，
   * refreshCaptcha() 因此触发两次、产生两个 uuid；页面 state 里留下的是最后那个。
   * 只抓第一个响应就会拿到已被覆盖的 uuid，答案对不上 → 登录失败且验证码框被清空。
   */
  const uuids: string[] = [];
  page.on('response', async (r) => {
    if (!r.url().includes('/api/v1/auth/captcha') || r.request().method() !== 'GET') return;
    try {
      const u = (await r.json())?.data?.uuid;
      if (u) uuids.push(u);
    } catch {
      /* 非 JSON 响应忽略 */
    }
  });

  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  // 后端关掉验证码时前端不渲染这个输入框
  if ((await page.locator('#defaultLogin_captcha').count()) === 0) return null;

  // 等到至少有一次验证码响应被记录（StrictMode 下通常是两次）
  await expect.poll(() => uuids.length, { timeout: 10000 }).toBeGreaterThan(0);
  const uuid = uuids[uuids.length - 1];
  if (!uuid) throw new Error('验证码接口未返回 uuid，无法完成 UI 登录');

  const answer = readCaptchaAnswer(uuid);
  // 不静默跳过：取不到答案就必然登录失败，直接报清楚原因，
  // 否则表现为「验证码框空着、登录卡住」，很难查
  if (!answer) {
    throw new Error(
      `未能从 Redis 读到验证码答案（key=captcha:${uuid}）。\n` +
        '确认 redis-cli 可用、且连的是后端所用的那个 Redis。',
    );
  }
  return answer;
}

/**
 * 走真实 UI 表单登录（含验证码）。
 * 只在 01-login.spec.ts 里用 —— 登录接口有 IP 限流（10 次/60 秒），
 * 其他用例请用 `login()`（复用 global-setup 预登录的 Token）。
 */
export async function loginViaUi(page: Page, username = 'admin', password = 'Abc@123456') {
  const captcha = await gotoLoginAndGetCaptcha(page);

  // Ant Design Form 的 input 通过 id 定位：#defaultLogin_username, #defaultLogin_password
  await page.locator('#defaultLogin_username').fill(username);
  await page.locator('#defaultLogin_password').fill(password);
  if (captcha) await page.locator('#defaultLogin_captcha').fill(captcha);

  // 先挂上响应等待再点击，否则快响应会在 await 之前就到达
  const loginResp = page.waitForResponse(
    (r) => r.url().includes('/api/v1/auth/login') && r.request().method() === 'POST',
    { timeout: 15000 },
  );
  await page.locator('button[type="submit"]').first().click();

  // 断言业务码而不是直接等跳转：登录被拒时（验证码过期 20020、限流 40001、
  // 账号锁定…）页面就是停在 /login，只等 URL 的话只能拿到一句 15s 超时，
  // 查不出到底是哪一种。
  const body = await (await loginResp).json().catch(() => null);
  if (!body || body.code !== 0) {
    throw new Error(
      `UI 登录被拒（${username}）：${JSON.stringify(body)}\n` +
        'code 20020=验证码错误/过期，40001=登录接口 IP 限流（10 次/60 秒），' +
        '20014=账号锁定（redis-cli del "login_fail:'.concat(username, '" 解锁）'),
    );
  }

  // 登录成功后跳到 '/'，由 App.tsx 依据动态菜单重定向到第一条路由
  await page.waitForURL(/\/(home|system|monitor|monitor-center)/, { timeout: 15000 });
}

function readToken(username: string): string {
  if (!fs.existsSync(TOKENS_FILE)) {
    throw new Error(`未找到 ${TOKENS_FILE}，global-setup 没跑起来？`);
  }
  const tokens = JSON.parse(fs.readFileSync(TOKENS_FILE, 'utf-8')) as Record<string, string>;
  const token = tokens[username];
  if (!token) {
    throw new Error(`global-setup 未预登录账号 ${username}，请在 e2e/global-setup.ts 的 ACCOUNTS 里补上`);
  }
  return token;
}

/**
 * 以指定账号进入已登录状态（复用预登录 Token，不打登录接口）。
 * 落地页由后端菜单决定：admin/chenli 都是「工作台」`/home`。
 */
export async function login(page: Page, username = 'admin', _password?: string) {
  const token = readToken(username);
  await page.addInitScript(
    ([key, value]) => window.localStorage.setItem(key, value),
    [TOKEN_KEY, token] as const,
  );
  await page.goto('/');
  await waitReady(page);
}

/**
 * 确保已登录状态（默认 admin）
 */
export async function ensureLoggedIn(page: Page, username = 'admin') {
  await login(page, username);
}

/**
 * 等 App.tsx 的「恢复登录状态」loading 门消失。
 *
 * page.goto 是整页刷新，Zustand 内存 store 会重置成「有 token 但没有 userInfo/menus」，
 * 此时 App 先渲染 loading 门再重新拉菜单。不等它，断言就会打在 loading 界面上。
 */
async function waitReady(page: Page) {
  // 用 testid 而不是文案：文案进了语言包，英文环境下按中文断言会永远为真、等于没等
  await expect(page.getByTestId('session-restore')).toHaveCount(0, { timeout: 15000 });
  await page.waitForLoadState('networkidle');
}

/**
 * 直接用 URL 导航到某个页面（整页刷新 + 等 loading 门）
 */
export async function gotoPage(page: Page, path: string) {
  await page.goto(path);
  await waitReady(page);
  await expect(page).toHaveURL(new RegExp(`${path}$`), { timeout: 15000 });
}

/**
 * 等待 Ant Design 消息提示
 */
export async function waitForMessage(page: Page, text: string) {
  await expect(page.locator('.ant-message').getByText(text)).toBeVisible({ timeout: 5000 });
}

/**
 * 等待表格加载完成
 */
export async function waitForTableLoaded(page: Page) {
  await page.waitForFunction(() => {
    const spin = document.querySelector('.ant-spin-spinning');
    return !spin;
  }, { timeout: 10000 });
}
