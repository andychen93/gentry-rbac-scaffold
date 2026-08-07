#!/usr/bin/env bash
# 查看前后端 + 依赖服务状态
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs/dev"

DB_NAME="${DB_NAME:-precision}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-123456}"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'
row() {
  local name=$1 status=$2 detail=$3
  if [[ "$status" == "UP" ]]; then
    printf "  ${GREEN}●${NC} %-16s ${GREEN}UP${NC}   %s\n"   "$name" "$detail"
  else
    printf "  ${RED}●${NC} %-16s ${RED}DOWN${NC} %s\n" "$name" "$detail"
  fi
}

echo "==================== 状态 ===================="

# PostgreSQL
if command -v psql >/dev/null 2>&1 \
   && PGPASSWORD="$DB_PASSWORD" psql -h localhost -U "$DB_USER" -d "$DB_NAME" -c 'SELECT 1' >/dev/null 2>&1; then
  row "PostgreSQL" "UP" "localhost:5432/$DB_NAME (psql 已验证)"
elif nc -z localhost 5432 2>/dev/null; then
  row "PostgreSQL" "UP" "localhost:5432 (端口通，未做 SQL 校验)"
else
  row "PostgreSQL" "DOWN" "localhost:5432"
fi

# Redis
if redis-cli ping 2>/dev/null | grep -q PONG; then
  BL=$(redis-cli --scan --pattern 'blacklist:*' 2>/dev/null | wc -l | tr -d ' ')
  SA=$(redis-cli --scan --pattern 'Authorization:*' 2>/dev/null | wc -l | tr -d ' ')
  row "Redis" "UP" "localhost:6379  sa-keys=$SA  blacklist=$BL"
else
  row "Redis" "DOWN" "localhost:6379"
fi

# Backend
if curl -sS -o /dev/null -w '%{http_code}' http://localhost:9090/api/v1/tenants/options 2>/dev/null | grep -qE '^(200|401|403)$'; then
  row "Backend API" "UP" "http://localhost:9090"
else
  row "Backend API" "DOWN" "http://localhost:9090"
fi

# Frontend
if curl -sS -o /dev/null -w '%{http_code}' http://localhost:3030 2>/dev/null | grep -qE '^(200|304)$'; then
  row "Frontend Vite" "UP" "http://localhost:3030"
else
  row "Frontend Vite" "DOWN" "http://localhost:3030"
fi

echo ""
[[ -f "$LOG_DIR/backend.pid" ]]  && echo "  后端 PID: $(cat "$LOG_DIR/backend.pid")"
[[ -f "$LOG_DIR/frontend.pid" ]] && echo "  前端 PID: $(cat "$LOG_DIR/frontend.pid")"
echo "  日志目录: $LOG_DIR"
