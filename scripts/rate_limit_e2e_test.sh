#!/usr/bin/env bash
# 接口限流 + 重复提交防护 E2E 验证
#
# 前置：后端刚启动（Caffeine 缓存为空），否则可能受上一轮计数影响
set -e
BASE=http://localhost:9090/api/v1

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass() { echo -e "  ${GREEN}✓ PASS${NC} $1"; }
fail() { echo -e "  ${RED}✗ FAIL${NC} $1"; EXIT_CODE=1; }
info() { echo -e "  ${YELLOW}→${NC} $1"; }
EXIT_CODE=0

echo "========== 限流 + 防重提交 E2E 验证 =========="

echo "【场景 1】改密码防重复提交：相同请求 5 秒内第二次返回 40002"
TOKEN=$(curl -sS -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Abc@123456"}' \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
info "Token: ${TOKEN:0:30}..."

BODY='{"oldPassword":"Abc@123456","newPassword":"Abc@123456"}'
CODE1=$(curl -sS -o /tmp/pw1.json -w "%{http_code}" \
    -X PUT -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" -d "$BODY" $BASE/auth/password)
info "第 1 次改密码: HTTP $CODE1: $(cat /tmp/pw1.json)"
BIZ1=$(python3 -c "import json; print(json.load(open('/tmp/pw1.json'))['code'])")
[[ "$BIZ1" == "0" ]] && pass "第 1 次请求正常处理" || fail "第 1 次请求失败 code=$BIZ1"

# 第 1 次改密码会踢人，需要重新登录
TOKEN2=$(curl -sS -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Abc@123456"}' \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

CODE2=$(curl -sS -o /tmp/pw2.json -w "%{http_code}" \
    -X PUT -H "Authorization: Bearer $TOKEN2" \
    -H "Content-Type: application/json" -d "$BODY" $BASE/auth/password)
BIZ2=$(python3 -c "import json; print(json.load(open('/tmp/pw2.json'))['code'])")
info "第 2 次改密码（5s 内）: HTTP $CODE2: $(cat /tmp/pw2.json)"
[[ "$BIZ2" == "40002" ]] && pass "防重复提交触发，业务码 40002" || fail "期望 40002 实际 $BIZ2"
echo ""

echo "【场景 2】登录接口限流：60 秒内 10 次，第 11 次返回 40001"
# 连续 10 次错误登录 + 第 11 次
for i in $(seq 1 10); do
    CODE=$(curl -sS -o /tmp/l.json -w "%{http_code}" \
        -X POST $BASE/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"nobody","password":"wrong"}')
    BIZ=$(python3 -c "import json; print(json.load(open('/tmp/l.json'))['code'])")
    echo "   第 $i 次: HTTP $CODE, code=$BIZ"
done

CODE=$(curl -sS -o /tmp/l.json -w "%{http_code}" \
    -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"nobody","password":"wrong"}')
BIZ=$(python3 -c "import json; print(json.load(open('/tmp/l.json'))['code'])")
info "第 11 次: HTTP $CODE: $(cat /tmp/l.json)"
[[ "$BIZ" == "40001" ]] && pass "限流触发，业务码 40001" || fail "期望 40001 实际 $BIZ"
echo ""

echo "========== 验证结束 =========="
exit $EXIT_CODE
