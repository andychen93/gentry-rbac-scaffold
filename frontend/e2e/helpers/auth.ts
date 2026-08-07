import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import fs from 'node:fs';
import { TOKENS_FILE } from '../global-setup';

const TOKEN_KEY = 'precision_token';

/**
 * 走真实 UI 表单登录。
 * 只在 01-login.spec.ts 里用 —— 登录接口有 IP 限流（10 次/60 秒），
 * 其他用例请用 `login()`（复用 global-setup 预登录的 Token）。
 */
export async function loginViaUi(page: Page, username = 'admin', password = 'Abc@123456') {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  // Ant Design Form 的 input 通过 id 定位：#defaultLogin_username, #defaultLogin_password
  await page.locator('#defaultLogin_username').fill(username);
  await page.locator('#defaultLogin_password').fill(password);
  await page.locator('button[type="submit"]').first().click();

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
  await expect(page.getByText('恢复登录状态...')).toHaveCount(0, { timeout: 15000 });
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
