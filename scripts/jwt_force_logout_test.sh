#!/bin/bash
# 场景 6：管理员强制下线用户
BASE=http://localhost:9090/api/v1

echo "=== 场景 6：管理员强制下线 ==="

# 超管登录
ADMIN_TOKEN=$(curl -sS -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chenli","password":"Chenli@2026"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo "超管 Token: ${ADMIN_TOKEN:0:30}..."

# 普通用户 admin 登录
USER_TOKEN=$(curl -sS -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Abc@123456"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo "用户 Token: ${USER_TOKEN:0:30}..."
echo ""

# 查在线用户
ONLINE=$(curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" $BASE/online-users)
echo "在线用户列表 (前200字符): ${ONLINE:0:200}..."
TOKEN_ID=$(echo "$ONLINE" | python3 -c "
import sys,json
d=json.load(sys.stdin)
for u in d.get('data', []):
    if u.get('username')=='admin':
        print(u['tokenId']); break
")
echo "待强制下线 tokenId(sessionId): $TOKEN_ID"
echo ""

# 强制下线
FL_CODE=$(curl -sS -o /tmp/fl.json -w "%{http_code}" \
  -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" \
  "$BASE/online-users/$TOKEN_ID")
echo "强制下线 HTTP $FL_CODE: $(cat /tmp/fl.json)"
echo ""

# 被踢用户再请求
AFTER=$(curl -sS -o /tmp/aft.json -w "%{http_code}" \
  -H "Authorization: Bearer $USER_TOKEN" $BASE/auth/user-info)
echo "被踢用户再次访问 HTTP $AFTER: $(cat /tmp/aft.json)"
echo ""

echo "Redis blacklist keys:"
redis-cli --scan --pattern 'blacklist:*'
