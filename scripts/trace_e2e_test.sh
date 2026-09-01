#!/usr/bin/env bash
# 全链路追踪 + 请求日志过滤器 E2E 验证
set -e
BASE=http://localhost:9090/api/v1

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass() { echo -e "  ${GREEN}✓ PASS${NC} $1"; }
fail() { echo -e "  ${RED}✗ FAIL${NC} $1"; EXIT_CODE=1; }
info() { echo -e "  ${YELLOW}→${NC} $1"; }
EXIT_CODE=0

echo "========== 全链路追踪 + 请求日志 E2E =========="

echo "【场景 1】响应携带 X-Trace-Id 头，R.traceId 与之一致"
curl -sS -D /tmp/h.txt -o /tmp/b.json $BASE/auth/captcha > /dev/null
HDR=$(grep -i "^x-trace-id:" /tmp/h.txt | awk '{print $2}' | tr -d '\r')
BODY_ID=$(python3 -c "import json; print(json.load(open('/tmp/b.json'))['traceId'])")
info "响应头 X-Trace-Id=$HDR"
info "R.traceId      =$BODY_ID"
[[ -n "$HDR" && "$HDR" == "$BODY_ID" ]] && pass "X-Trace-Id 与 R.traceId 一致" || fail "不一致"
echo ""

echo "【场景 2】上游请求头 X-Trace-Id 透传"
CUSTOM="e2e-test-$(date +%s)"
curl -sS -D /tmp/h2.txt -H "X-Trace-Id: $CUSTOM" -o /tmp/b2.json $BASE/auth/captcha > /dev/null
R_ID=$(python3 -c "import json; print(json.load(open('/tmp/b2.json'))['traceId'])")
info "上游指定 traceId=$CUSTOM, R.traceId=$R_ID"
[[ "$R_ID" == "$CUSTOM" ]] && pass "上游 traceId 透传成功" || fail "未透传"
echo ""

echo "【场景 3】请求日志过滤器记录请求行 + password 脱敏"
curl -sS -X POST -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"Abc@123456"}' \
    -o /dev/null $BASE/auth/login

# 查看日志中最新一行 RequestLogFilter 记录
LOG_LINE=$(grep "auth/login" logs/dev/backend.log | tail -1)
info "日志: $LOG_LINE"
echo "$LOG_LINE" | grep -q 'password":"\*\*\*"' && pass "password 已脱敏为 ***" || fail "password 未脱敏"
echo "$LOG_LINE" | grep -q "body=" && pass "body 已记录" || fail "body 未记录"
echo ""

echo "【场景 4】日志 pattern 含 traceId"
grep "c.p.core.web.RequestLogFilter" logs/dev/backend.log | tail -3
ANY=$(grep "c.p.core.web.RequestLogFilter.*\\[[0-9a-f]\\{32\\}\\]" logs/dev/backend.log | wc -l | tr -d ' ')
[[ "$ANY" -gt "0" ]] && pass "日志 pattern 已启用 traceId MDC（至少 $ANY 条）" || fail "日志无 traceId"
echo ""

echo "========== 验证结束 =========="
exit $EXIT_CODE
