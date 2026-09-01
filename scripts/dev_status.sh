#!/usr/bin/env bash
# 查看前后端 + 依赖服务状态（自动探测 MySQL/PostgreSQL 哪个端口通，不要求提前知道用的是哪个）
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs/dev"

DB_NAME="${DB_NAME:-precision}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
PG_USER="${PG_USER:-postgres}"
PG_PASSWORD="${PG_PASSWORD:-123456}"

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

# MySQL
if command -v mysql >/dev/null 2>&1 \
   && mysql -h127.0.0.1 -P3306 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e 'SELECT 1' >/dev/null 2>&1; then
  row "MySQL" "UP" "localhost:3306/$DB_NAME (mysql 已验证)"
elif nc -z localhost 3306 2>/dev/null; then
  row "MySQL" "UP" "localhost:3306 (端口通，未做 SQL 校验)"
else
  row "MySQL" "DOWN" "localhost:3306"
fi

# PostgreSQL
if command -v psql >/dev/null 2>&1 \
   && PGPASSWORD="$PG_PASSWORD" psql -h localhost -U "$PG_USER" -d "$DB_NAME" -c 'SELECT 1' >/dev/null 2>&1; then
  row "PostgreSQL" "UP" "localhost:5432/$DB_NAME (psql 已验证)"
elif nc -z localhost 5432 2>/dev/null; then
  row "PostgreSQL" "UP" "localhost:5432 (端口通，未做 SQL 校验)"
else
  row "PostgreSQL" "DOWN" "localhost:5432"
fi

# SQLite（文件型库，检查后端工作目录下有没有落盘文件）
if [[ -f "$ROOT_DIR/backend/gentry-start/gentry.db" ]]; then
  row "SQLite" "UP" "backend/gentry-start/gentry.db"
else
  row "SQLite" "DOWN" "未找到 gentry.db（尚未用 --db=sqlite 启动过）"
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
if curl -sS -o /dev/null -w '%{http_code}' http://localhost:9090/api/v1/auth/captcha 2>/dev/null | grep -qE '^(200|401|403)$'; then
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
