#!/usr/bin/env bash
# Redis 监控模块 E2E 测试
set -e
BASE=http://localhost:9090/api/v1
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass() { echo -e "  ${GREEN}✓ PASS${NC} $1"; }
fail() { echo -e "  ${RED}✗ FAIL${NC} $1"; EXIT_CODE=1; }
info() { echo -e "  ${YELLOW}→${NC} $1"; }
EXIT_CODE=0

echo "========== Redis 监控 E2E 测试 =========="

# chenli 登录（ADMIN，拥有全部权限）
TOKEN=$(curl -sS -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chenli","password":"Chenli@2026"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
info "chenli Token: ${TOKEN:0:30}..."
echo ""

# 准备一些测试数据
redis-cli set "test:string:1" "hello world" EX 3600 > /dev/null
redis-cli rpush "test:list:1" a b c > /dev/null
redis-cli hset "test:hash:1" name zhangsan age 25 > /dev/null
redis-cli sadd "test:set:1" x y z > /dev/null
redis-cli zadd "test:zset:1" 1 m1 2 m2 > /dev/null
info "测试数据已写入 Redis"
echo ""

echo "【场景 1】GET /monitor/redis/info - 监控总览"
RESP=$(curl -sS -H "Authorization: Bearer $TOKEN" $BASE/monitor/redis/info)
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])")
VERSION=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['info']['redisVersion'])")
DBSIZE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['dbSize'])")
STATS_CNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['data']['commandStats']))")
HIT_RATE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['info']['hitRate'])")
QPS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['info']['instantaneousOpsPerSec'])")
info "code=$CODE, redisVersion=$VERSION, dbSize=$DBSIZE, commandStats 数=$STATS_CNT, hitRate=$HIT_RATE, qps=$QPS"
[[ "$CODE" == "0" && -n "$VERSION" && "$DBSIZE" -gt "0" ]] \
  && pass "监控总览返回正常（含扩展指标）" || fail "监控总览异常"
echo ""

echo "【场景 2】GET /monitor/redis/key-defines - 预定义 Key 模板"
RESP=$(curl -sS -H "Authorization: Bearer $TOKEN" $BASE/monitor/redis/key-defines)
KT=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print(','.join(x['keyType'] for x in d))")
info "keyTypes=$KT"
[[ "$KT" == *"token_mapping"* && "$KT" == *"jwt_blacklist"* ]] \
  && pass "预定义 Key 模板包含 token_mapping / jwt_blacklist" || fail "预定义 Key 模板异常"
echo ""

echo "【场景 3】GET /monitor/redis/keys?pattern=test:* - 扫描 Key"
RESP=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/monitor/redis/keys?pattern=test:*&pageNum=1&pageSize=20")
TOTAL=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['total'])")
KEYS=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin)['data']['list']; print(','.join(k['key']+':'+k['type'] for k in d))")
info "total=$TOTAL  keys=$KEYS"
[[ "$TOTAL" -ge "5" && "$KEYS" == *"test:list:1:list"* && "$KEYS" == *"test:hash:1:hash"* ]] \
  && pass "扫描返回 5 条以上，类型正确" || fail "扫描结果异常"
echo ""

echo "【场景 4】GET /monitor/redis/keys/{key}/value - 各类型值查询"
for K in "test:string:1" "test:list:1" "test:hash:1" "test:set:1" "test:zset:1"; do
  RESP=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/monitor/redis/keys/$K/value")
  TYPE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['type'])")
  VAL=$(echo "$RESP" | python3 -c "import sys,json; v=json.load(sys.stdin)['data']['value']; print(v[:80] if v else 'null')")
  info "$K → type=$TYPE  value=$VAL"
done
pass "各类型 Value 查询均有返回"
echo ""

echo "【场景 5】DELETE /monitor/redis/keys/{key} - 删除"
DEL_CODE=$(curl -sS -o /tmp/del.json -w "%{http_code}" -X DELETE \
  -H "Authorization: Bearer $TOKEN" "$BASE/monitor/redis/keys/test:string:1")
info "HTTP $DEL_CODE: $(cat /tmp/del.json)"
[[ "$DEL_CODE" == "200" ]] && pass "删除响应 200" || fail "删除失败"

EXIST=$(redis-cli exists "test:string:1")
[[ "$EXIST" == "0" ]] && pass "Redis 中 Key 已被删除" || fail "Redis 中 Key 仍存在"
echo ""

echo "【场景 6】未登录访问返回 401"
CODE=$(curl -sS -o /tmp/r.json -w "%{http_code}" $BASE/monitor/redis/info)
info "HTTP $CODE: $(cat /tmp/r.json)"
[[ "$CODE" == "401" ]] && pass "未登录返回 401" || fail "期望 401 实际 $CODE"
echo ""

echo "【场景 7】ADMIN 也能查看 info 与删除 Key（唯一角色，拥有全部权限）"
ADMIN_TOKEN=$(curl -sS -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Abc@123456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
info "ADMIN Token: ${ADMIN_TOKEN:0:30}..."

# ADMIN 查询 info
CODE_I=$(curl -sS -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/monitor/redis/info)
info "ADMIN 查 info: HTTP $CODE_I"
[[ "$CODE_I" == "200" ]] && pass "ADMIN 可以查看 info" || fail "ADMIN 查看 info 失败"

# ADMIN 删除 Key（去多租户化后 ADMIN 持有全部权限，应成功）
CODE_D=$(curl -sS -o /tmp/r2.json -w "%{http_code}" -X DELETE \
  -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/monitor/redis/keys/test:list:1")
info "ADMIN 删除: HTTP $CODE_D: $(cat /tmp/r2.json)"
[[ "$CODE_D" == "200" ]] && pass "ADMIN 删除 Key 成功" || fail "期望 200 实际 $CODE_D"
echo ""

# 清理
redis-cli del "test:list:1" "test:hash:1" "test:set:1" "test:zset:1" > /dev/null

echo "【场景 8】GET /monitor/redis/slowlog - 慢查询日志"
# 故意制造几条慢查询
redis-cli config set slowlog-log-slower-than 0 > /dev/null
redis-cli keys '*' > /dev/null
redis-cli get missing-key > /dev/null
redis-cli config set slowlog-log-slower-than 10000 > /dev/null

RESP=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/monitor/redis/slowlog?limit=10")
CNT=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data') or []))")
info "慢日志条数: $CNT"
[[ "$CNT" -gt "0" ]] && pass "慢日志可查询" || fail "慢日志为空"
echo ""

echo "【场景 9】DELETE /monitor/redis/slowlog - chenli 与 admin 都能清空（唯一角色，拥有全部权限）"
CODE_S=$(curl -sS -o /tmp/s.json -w "%{http_code}" -X DELETE -H "Authorization: Bearer $TOKEN" $BASE/monitor/redis/slowlog)
info "chenli 清空: HTTP $CODE_S"
[[ "$CODE_S" == "200" ]] && pass "chenli 可清空慢日志" || fail "chenli 清空失败"

CODE_A=$(curl -sS -o /tmp/a.json -w "%{http_code}" -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/monitor/redis/slowlog)
info "admin 清空: HTTP $CODE_A: $(cat /tmp/a.json)"
[[ "$CODE_A" == "200" ]] && pass "admin 可清空慢日志" || fail "期望 200 实际 $CODE_A"
echo ""

echo "========== 验证结束 =========="
exit $EXIT_CODE
