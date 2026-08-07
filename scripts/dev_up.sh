#!/usr/bin/env bash
# RBAC 脚手架一键启动
# - 检查数据库 / Redis 依赖
# - 编译后端（可跳过）
# - 后台启动后端 + 前端
# - 等待健康检查
#
# 用法:
#   bash scripts/dev_up.sh                 # 默认启动（db=mysql，JWT 模式，需要 Redis）
#   bash scripts/dev_up.sh --db=postgresql # 切到 PostgreSQL（对应 bash scripts/deps_up.sh --db=postgresql）
#   bash scripts/dev_up.sh --db=sqlite     # 切到 SQLite（文件型库，不需要 deps_up.sh）
#   bash scripts/dev_up.sh --skip-build    # 跳过后端编译（直接用已有 jar）
#   bash scripts/dev_up.sh --uuid          # 用 UUID Token 模式启动（不依赖 Redis）
#   bash scripts/dev_up.sh --only-backend
#   bash scripts/dev_up.sh --only-frontend

set -e

# ============ 路径 ============
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
LOG_DIR="$ROOT_DIR/logs/dev"
BACKEND_JAR="$BACKEND_DIR/precision-start/target/precision-start-1.0.0-SNAPSHOT.jar"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

# ============ 参数 ============
SKIP_BUILD=0
TOKEN_STYLE="jwt-simple"
ONLY_BACKEND=0
ONLY_FRONTEND=0
DB_PROFILE="mysql"

for arg in "$@"; do
  case "$arg" in
    --skip-build)    SKIP_BUILD=1 ;;
    --uuid)          TOKEN_STYLE="uuid" ;;
    --only-backend)  ONLY_BACKEND=1 ;;
    --only-frontend) ONLY_FRONTEND=1 ;;
    --db=*)          DB_PROFILE="${arg#--db=}" ;;
    -h|--help)
      grep -E '^# ' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "未知参数: $arg（-h 查看帮助）"; exit 1 ;;
  esac
done

case "$DB_PROFILE" in
  mysql|postgresql|sqlite) ;;
  *) echo "--db 只支持 mysql | postgresql | sqlite（当前: $DB_PROFILE）"; exit 1 ;;
esac

# 按 profile 定连接检查用的默认账号/端口，可用环境变量覆盖
if [[ "$DB_PROFILE" == "mysql" ]]; then
  DB_PORT="${DB_PORT:-3306}"; DB_NAME="${DB_NAME:-precision}"; DB_USER="${DB_USER:-root}"; DB_PASSWORD="${DB_PASSWORD:-123456}"
elif [[ "$DB_PROFILE" == "postgresql" ]]; then
  DB_PORT="${DB_PORT:-5432}"; DB_NAME="${DB_NAME:-precision}"; DB_USER="${DB_USER:-postgres}"; DB_PASSWORD="${DB_PASSWORD:-123456}"
fi

mkdir -p "$LOG_DIR"

# ============ 颜色 ============
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERR ]${NC} $*"; }

# ============ 依赖检查 ============
check_database() {
  if [[ "$DB_PROFILE" == "sqlite" ]]; then
    info "SQLite 是文件型库，无需连通性检查（首次启动 Flyway 自动建 precision.db）"
    return 0
  fi

  info "检查 ${DB_PROFILE} (localhost:$DB_PORT, db=$DB_NAME)"
  if [[ "$DB_PROFILE" == "mysql" ]] && command -v mysql >/dev/null 2>&1 \
     && mysql -h127.0.0.1 -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e 'SELECT 1' >/dev/null 2>&1; then
    ok "MySQL 连接成功"
    return 0
  fi
  if [[ "$DB_PROFILE" == "postgresql" ]] && command -v psql >/dev/null 2>&1 \
     && PGPASSWORD="$DB_PASSWORD" psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c 'SELECT 1' >/dev/null 2>&1; then
    ok "PostgreSQL 连接成功"
    return 0
  fi
  # 客户端不可用时退回端口探测
  if command -v nc >/dev/null 2>&1 && nc -z localhost "$DB_PORT" 2>/dev/null; then
    ok "端口 $DB_PORT 连通（未做 SQL 校验，依赖后端启动时报错暴露）"
    return 0
  fi
  error "无法连接到 $DB_PROFILE (localhost:$DB_PORT)"
  error "最快的办法： bash scripts/deps_up.sh --db=$DB_PROFILE   （用 Docker 起数据库 + Redis）"
  return 1
}

check_redis() {
  if [[ "$TOKEN_STYLE" == "uuid" ]]; then
    info "UUID 模式，跳过 Redis 检查"
    return 0
  fi
  info "检查 Redis (localhost:6379)"
  if ! command -v redis-cli >/dev/null 2>&1; then
    warn "redis-cli 未安装，跳过 Redis 连通性检查"
    return 0
  fi
  if redis-cli ping 2>/dev/null | grep -q PONG; then
    ok "Redis 连接成功"
  else
    error "Redis 未运行，JWT 模式依赖 Redis 存 Session 与 Token 黑名单"
    error "启动方式： bash scripts/deps_up.sh   或  brew services start redis"
    error "临时绕过： 加 --uuid 参数退化到 UUID 模式"
    return 1
  fi
}

check_port() {
  local port=$1 name=$2
  if lsof -i ":$port" -t >/dev/null 2>&1; then
    error "端口 $port ($name) 已被占用"
    error "处理办法：bash scripts/dev_down.sh  或手动 kill：lsof -i :$port -t | xargs kill -9"
    return 1
  fi
}

# ============ 构建 & 启动 ============
build_backend() {
  if [[ $SKIP_BUILD -eq 1 ]]; then
    info "跳过构建（--skip-build）"
    if [[ ! -f "$BACKEND_JAR" ]]; then
      error "未找到已构建的 jar：$BACKEND_JAR"
      error "请去掉 --skip-build 参数重新运行"
      return 1
    fi
    return 0
  fi
  info "编译后端（mvn clean package -DskipTests）"
  (cd "$BACKEND_DIR" && mvn clean package -DskipTests -q)
  ok "后端编译完成"
}

start_backend() {
  info "启动后端 db=$DB_PROFILE token-style=$TOKEN_STYLE"
  check_port 9090 "HTTP API" || return 1

  nohup java \
    "-Dspring.profiles.active=$DB_PROFILE" \
    "-Dsa-token.token-style=$TOKEN_STYLE" \
    -jar "$BACKEND_JAR" \
    > "$BACKEND_LOG" 2>&1 &
  local pid=$!
  echo "$pid" > "$BACKEND_PID_FILE"
  info "后端进程已启动 PID=$pid"
  info "后端日志 $BACKEND_LOG"

  info "等待后端就绪……"
  for i in $(seq 1 60); do
    if curl -sS -o /dev/null -w '%{http_code}' http://localhost:9090/api/v1/tenants/options 2>/dev/null | grep -qE '^(200|401|403)$'; then
      ok "后端就绪 http://localhost:9090"
      return 0
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      error "后端进程已退出，最近日志："
      tail -40 "$BACKEND_LOG"
      return 1
    fi
    sleep 1
  done
  error "后端 60s 内未就绪，检查日志：$BACKEND_LOG"
  tail -40 "$BACKEND_LOG"
  return 1
}

start_frontend() {
  info "启动前端 npm run dev"
  check_port 3030 "Vite Dev Server" || return 1

  if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
    info "首次运行，安装前端依赖（npm install）"
    (cd "$FRONTEND_DIR" && npm install)
  fi

  (cd "$FRONTEND_DIR" && nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
   echo $! > "$FRONTEND_PID_FILE")
  local pid
  pid=$(cat "$FRONTEND_PID_FILE")
  info "前端进程已启动 PID=$pid"
  info "前端日志 $FRONTEND_LOG"

  info "等待前端就绪……"
  for i in $(seq 1 45); do
    if curl -sS -o /dev/null -w '%{http_code}' http://localhost:3030 2>/dev/null | grep -qE '^(200|304)$'; then
      ok "前端就绪 http://localhost:3030"
      return 0
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      error "前端进程已退出，最近日志："
      tail -40 "$FRONTEND_LOG"
      return 1
    fi
    sleep 1
  done
  error "前端 45s 内未就绪，检查日志：$FRONTEND_LOG"
  return 1
}

# ============ 流程 ============
echo -e "${BLUE}==================== RBAC 脚手架 Dev Up ====================${NC}"
info "工作目录：$ROOT_DIR"
info "数据库：  $DB_PROFILE"
info "Token 模式：$TOKEN_STYLE"
info "日志目录：$LOG_DIR"
echo ""

if [[ $ONLY_FRONTEND -eq 0 ]]; then
  check_database || exit 1
  check_redis    || exit 1
  build_backend  || exit 1
  start_backend  || exit 1
fi

if [[ $ONLY_BACKEND -eq 0 ]]; then
  start_frontend || exit 1
fi

echo ""
echo -e "${GREEN}==================== 启动完成 ====================${NC}"
echo "  后端 API:      http://localhost:9090"
echo "  前端 Web:      http://localhost:3030"
echo "  后端日志:      tail -f $BACKEND_LOG"
echo "  前端日志:      tail -f $FRONTEND_LOG"
echo "  停止所有:      bash scripts/dev_down.sh"
echo ""
echo "  默认账号：     admin / Abc@123456  （租户管理员）"
echo "                 chenli / Chenli@2026（平台超管，可见租户管理）"
