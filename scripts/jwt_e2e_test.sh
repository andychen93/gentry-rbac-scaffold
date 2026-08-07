#!/bin/bash
# JWT + Redis 黑名单 E2E 验证脚本
BASE=http://localhost:9090/api/v1
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

pass() { echo -e "  ${GREEN}✓ PASS${NC} $1"; }
fail() { echo -e "  ${RED}✗ FAIL${NC} $1"; EXIT_CODE=1; }
info() { echo -e "  ${YELLOW}→${NC} $1"; }

EXIT_CODE=0

echo "========== JWT + Redis 黑名单 E2E 验证 =========="
echo ""

echo "【场景 1】登录返回 JWT"
LOGIN=$(curl -sS -X POST $BASE/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Abc@123456"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
REFRESH=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['refreshToken'])")
info "Token 前缀: ${TOKEN:0:30}..."
info "Token 长度: ${#TOKEN}"
info "refreshToken: $REFRESH"
[[ "$TOKEN" == eyJ* ]] && pass "Token 以 eyJ 开头（JWT 格式）" || fail "Token 不是 JWT 格式"

DOTS=$(echo -n "$TOKEN" | tr -cd '.' | wc -c | tr -d ' ')
[[ "$DOTS" == "2" ]] && pass "JWT 格式有 3 段（header.payload.signature）" || fail "JWT 段数异常: $DOTS"

PAYLOAD=$(echo "$TOKEN" | cut -d'.' -f2)
case $((${#PAYLOAD} % 4)) in 2) PAYLOAD="${PAYLOAD}==" ;; 3) PAYLOAD="${PAYLOAD}=" ;; esac
PAYLOAD_JSON=$(echo "$PAYLOAD" | base64 -d 2>/dev/null)
info "JWT Payload: $PAYLOAD_JSON"
echo "$PAYLOAD_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d['loginType']=='login' and d['loginId']==2 and 'rnStr' in d" 2>/dev/null && pass "Payload 包含 loginType、loginId、rnStr" || fail "Payload 字段异常"
echo ""

echo "【场景 2】携带 JWT 访问受保护资源（预期 200）"
CODE1=$(curl -sS -o /tmp/u1.json -w "%{http_code}" -H "Authorization: Bearer $TOKEN" $BASE/auth/user-info)
USERNAME=$(python3 -c "import json; print(json.load(open('/tmp/u1.json'))['data']['username'])" 2>/dev/null)
info "HTTP $CODE1, username=$USERNAME"
[[ "$CODE1" == "200" && "$USERNAME" == "admin" ]] && pass "受保护资源返回 200 + 正确用户" || fail "访问失败"
echo ""

echo "【场景 3】观察 Redis 存储"
SA_KEYS=$(redis-cli --scan --pattern 'Authorization:*' | wc -l | tr -d ' ')
info "Sa-Token 相关 key 数: $SA_KEYS"
redis-cli --scan --pattern 'Authorization:*' | head -5 | sed 's/^/    /'
[[ "$SA_KEYS" -gt "0" ]] && pass "Redis 中存在 Sa-Token Session" || fail "Redis 中无 Sa-Token 数据"
echo ""

echo "【场景 4】登出后原 JWT 请求被拦截（预期 401）"
CODE_LO=$(curl -sS -o /tmp/lo.json -w "%{http_code}" -X POST -H "Authorization: Bearer $TOKEN" $BASE/auth/logout)
info "登出 HTTP $CODE_LO"
[[ "$CODE_LO" == "200" ]] && pass "登出成功" || fail "登出失败"

BL_KEYS=$(redis-cli --scan --pattern 'blacklist:*' | wc -l | tr -d ' ')
info "黑名单 key 数: $BL_KEYS"
redis-cli --scan --pattern 'blacklist:*' | head -3 | sed 's/^/    /'
[[ "$BL_KEYS" -gt "0" ]] && pass "黑名单 key 已写入 Redis" || fail "黑名单 key 未写入"

FIRST_BL=$(redis-cli --scan --pattern 'blacklist:*' | head -1)
if [[ -n "$FIRST_BL" ]]; then
  TTL=$(redis-cli ttl "$FIRST_BL")
  info "黑名单 TTL=$TTL 秒"
  [[ "$TTL" -gt "0" && "$TTL" -le "1800" ]] && pass "黑名单 TTL 在 (0, 1800] 范围内" || fail "TTL 异常: $TTL"
fi

CODE_AFTER=$(curl -sS -o /tmp/after.json -w "%{http_code}" -H "Authorization: Bearer $TOKEN" $BASE/auth/user-info)
info "登出后 HTTP $CODE_AFTER: $(cat /tmp/after.json)"
[[ "$CODE_AFTER" == "401" ]] && pass "登出后 JWT 访问返回 401" || fail "登出后仍可访问，HTTP $CODE_AFTER"
echo ""

echo "【场景 5】改密码后原 JWT 失效（预期 401）"
LOGIN2=$(curl -sS -X POST $BASE/auth/login -H "Content-Type: application/json" -d '{"username":"chenli","password":"Chenli@2026"}')
TOKEN2=$(echo "$LOGIN2" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['token'] if d.get('code')==0 else '')")

if [[ -n "$TOKEN2" ]]; then
  info "chenli 登录成功，原 Token: ${TOKEN2:0:30}..."
  CODE_PW=$(curl -sS -o /tmp/pw.json -w "%{http_code}" -X PUT \
    -H "Authorization: Bearer $TOKEN2" \
    -H "Content-Type: application/json" \
    -d '{"oldPassword":"Chenli@2026","newPassword":"Chenli@2026"}' \
    $BASE/auth/password)
  info "改密码 HTTP $CODE_PW: $(cat /tmp/pw.json)"

  CODE_AF2=$(curl -sS -o /tmp/after2.json -w "%{http_code}" -H "Authorization: Bearer $TOKEN2" $BASE/auth/user-info)
  info "改密码后 HTTP $CODE_AF2: $(cat /tmp/after2.json)"
  [[ "$CODE_AF2" == "401" ]] && pass "改密码后原 JWT 返回 401" || fail "改密码后仍可访问，HTTP $CODE_AF2"
else
  info "跳过：chenli 登录失败"
fi

echo ""
echo "========== 验证结束 =========="
exit $EXIT_CODE
