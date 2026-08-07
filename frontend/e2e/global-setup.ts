import { request } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

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

export default async function globalSetup() {
  const ctx = await request.newContext({ baseURL: 'http://localhost:9090' });
  const tokens: Record<string, string> = {};

  try {
    for (const [username, password] of Object.entries(ACCOUNTS)) {
      const res = await ctx.post('/api/v1/auth/login', { data: { username, password } });
      const body = await res.json();
      if (body.code !== 0 || !body.data?.token) {
        throw new Error(
          `预登录失败 ${username}: HTTP ${res.status()} ${JSON.stringify(body)}\n` +
            '请确认后端已启动（bash scripts/dev_up.sh），且内置账号密码未被改过。',
        );
      }
      tokens[username] = body.data.token;
    }
  } finally {
    await ctx.dispose();
  }

  fs.mkdirSync(path.dirname(TOKENS_FILE), { recursive: true });
  fs.writeFileSync(TOKENS_FILE, JSON.stringify(tokens, null, 2));
}
