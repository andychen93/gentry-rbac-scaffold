import { request } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

/**
 * 全局前置：一次性登录内置账号，把 Token 落盘给各 spec 复用。
 *
 * 为什么必须这么做：登录接口带 `@RateLimit(IP, count=10, period=60)`，
 * 如果每个 test 都走一次真实登录，整套用例会在一分钟内打满限流拿到 40001。
 * 这里只登录 2 次，其余用例直接把 Token 注入 localStorage。
 * 真实 UI 登录流程由 01-login.spec.ts 单独覆盖。
 */

// package.json 是 "type": "module"，没有 __dirname，用 import.meta.url 推算
const HERE = path.dirname(fileURLToPath(import.meta.url));

export const TOKENS_FILE = path.join(HERE, '.auth', 'tokens.json');

const ACCOUNTS: Record<string, string> = {
  admin: 'Abc@123456',
  chenli: 'Chenli@2026',
};

/** 验证码错误的业务码（ErrorCode.CAPTCHA_ERROR） */
const CAPTCHA_ERROR = 20020;

/**
 * 取验证码答案。
 *
 * 验证码答案只存在 Redis（key = `captcha:{uuid}`，见 CaptchaServiceImpl），
 * 接口只返回图片，所以测试环境直接用 redis-cli 读。仅测试用途。
 */
export function readCaptchaAnswer(uuid: string): string {
  return execFileSync('redis-cli', ['get', `captcha:${uuid}`], { encoding: 'utf-8' }).trim();
}

/**
 * ADMIN 按设计不该有的权限（租户管理属 SUPER_ADMIN；Redis 删除/清慢日志见 V3/V4 迁移）。
 * 多个用例（08 T-003、09 MON-REDIS-004/006、10 H-002）都建立在这个前提上。
 */
const ADMIN_FORBIDDEN = [
  'system:tenant:list',
  'system:tenant:config',
  'monitor:redis:key:delete',
  'monitor:redis:slowlog:reset',
];

/**
 * 前置校验：ADMIN 的权限没被改坏。
 *
 * 在「角色管理→权限」页面给 ADMIN 保存一次全选，就会让它拿到租户等越权权限，
 * 进而让上述用例莫名失败。这里提前拦住并给出可执行的修复指引，
 * 而不是让人去追 4 个看不懂的断言错误。
 */
async function assertAdminBaseline(
  ctx: Awaited<ReturnType<typeof request.newContext>>,
  adminToken: string,
) {
  const res = await ctx.get('/api/v1/auth/user-info', {
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  const perms: string[] = (await res.json())?.data?.permissions ?? [];
  const leaked = ADMIN_FORBIDDEN.filter((p) => perms.includes(p));
  if (leaked.length > 0) {
    throw new Error(
      `ADMIN 权限已被改坏，越权持有：${leaked.join(', ')}\n` +
        '多半是在「角色管理→权限」里给 ADMIN 保存过全选。\n' +
        '修复：bash scripts/db_reset.sh 重建库（Flyway 会按迁移重新灌种子数据），然后重启后端。',
    );
  }
}

export default async function globalSetup() {
  const ctx = await request.newContext({ baseURL: 'http://localhost:9090' });
  const tokens: Record<string, string> = {};

  try {
    for (const [username, password] of Object.entries(ACCOUNTS)) {
      // 先按无验证码登录（gentry.captcha.enabled=false 的部署）
      let res = await ctx.post('/api/v1/auth/login', { data: { username, password } });
      let body = await res.json();

      // 开了验证码就取一次答案重试
      if (body.code === CAPTCHA_ERROR) {
        const capRes = await ctx.get('/api/v1/auth/captcha');
        const capBody = await capRes.json();
        const uuid = capBody?.data?.uuid;
        if (!uuid) {
          throw new Error(`获取验证码失败: ${JSON.stringify(capBody)}`);
        }
        const captcha = readCaptchaAnswer(uuid);
        if (!captcha) {
          throw new Error(
            `未能从 Redis 读到验证码答案（key=captcha:${uuid}）。\n` +
              '确认 redis-cli 可用且连的是后端所用的那个 Redis；' +
              '或把后端以 -Dgentry.captcha.enabled=false 启动。',
          );
        }
        res = await ctx.post('/api/v1/auth/login', {
          data: { username, password, uuid, captcha },
        });
        body = await res.json();
      }

      if (body.code !== 0 || !body.data?.token) {
        throw new Error(
          `预登录失败 ${username}: HTTP ${res.status()} ${JSON.stringify(body)}\n` +
            '请确认后端已启动（bash scripts/dev_up.sh），且内置账号密码未被改过。',
        );
      }
      tokens[username] = body.data.token;
    }

    await assertAdminBaseline(ctx, tokens['admin']);
  } finally {
    await ctx.dispose();
  }

  fs.mkdirSync(path.dirname(TOKENS_FILE), { recursive: true });
  fs.writeFileSync(TOKENS_FILE, JSON.stringify(tokens, null, 2));
}
