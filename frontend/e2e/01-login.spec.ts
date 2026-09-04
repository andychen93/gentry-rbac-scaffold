import { test, expect } from '@playwright/test';
import { loginViaUi, gotoLoginAndGetCaptcha } from './helpers/auth';
import { pack } from './helpers/i18n';

const commonZh = pack('zh-CN', 'common');

test.describe('登录功能', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
  });

  test('登录页正常渲染', async ({ page }) => {
    await expect(page.getByText(commonZh['app.name'])).toBeVisible();
    await expect(page.locator('#login_username')).toBeVisible();
    await expect(page.locator('#login_password')).toBeVisible();
  });

  test('登录 - 正确账密', async ({ page }) => {
    await loginViaUi(page, 'admin', 'Abc@123456');
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('登录 - 错误密码', async ({ page }) => {
    // 验证码要填对，否则前端表单校验直接拦住、请求根本发不出去，
    // 这个用例就变成在验证「验证码必填」而不是「密码错误」了
    const captcha = await gotoLoginAndGetCaptcha(page);
    await page.locator('#login_username').fill('admin');
    await page.locator('#login_password').fill('WrongPassword1');
    if (captcha) await page.locator('#login_captcha').fill(captcha);
    await page.locator('button[type="submit"]').first().click();
    // 应提示错误消息，且停留在登录页
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 });
    await expect(page).toHaveURL(/\/login/);
  });

  test('登录 - 空用户名提交', async ({ page }) => {
    await page.locator('button[type="submit"]').first().click();
    await expect(page.getByText('请输入用户名')).toBeVisible({ timeout: 3000 });
  });
});
