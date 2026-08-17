#!/usr/bin/env bash
# 用 Docker 启动本地开发依赖：{MySQL|PostgreSQL} + Redis，并等待健康
#
# 用法:
#   bash scripts/deps_up.sh                 # 默认 MySQL + Redis
#   bash scripts/deps_up.sh --db=mysql       # 同上，显式指定
#   bash scripts/deps_up.sh --db=postgresql  # PostgreSQL + Redis
#
# SQLite 不需要这个脚本：它是文件型库，backend/gentry-start 目录下
# 切 profile=sqlite 直接跑就有，见 application-sqlite.yml。
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/docker-compose.yml"

DB="mysql"
for arg in "$@"; do
  case "$arg" in
    --db=*) DB="${arg#--db=}" ;;
    -h|--help)
      grep -E '^# ' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "未知参数: $arg（-h 查看帮助）"; exit 1 ;;
  esac
done

case "$DB" in
  mysql|postgresql) ;;
  *) echo "--db 只支持 mysql | postgresql（当前: $DB）"; exit 1 ;;
esac

GREEN='\033[0;32m'; RED='\033[0;31m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC} $*"; }
error() { echo -e "${RED}[ERR ]${NC} $*"; }

if ! command -v docker >/dev/null 2>&1; then
  error "未安装 docker。可改为本机安装对应数据库 + Redis 7，端口保持默认。"
  exit 1
fi

if [[ "$DB" == "mysql" ]]; then
  CONTAINER="rbac-mysql"; PORT=3306; DBNAME="MySQL"
else
  CONTAINER="rbac-postgres"; PORT=5432; DBNAME="PostgreSQL"
fi

info "启动 $DBNAME + Redis"
docker compose -f "$COMPOSE_FILE" --profile "$DB" up -d

info "等待健康检查……"
for i in $(seq 1 90); do
  dbh=$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo starting)
  rd=$(docker inspect -f '{{.State.Health.Status}}' rbac-redis 2>/dev/null || echo starting)
  if [[ "$dbh" == "healthy" && "$rd" == "healthy" ]]; then
    ok "$DBNAME   localhost:$PORT/precision"
    ok "Redis      localhost:6379"
    echo ""
    echo "下一步： bash scripts/dev_up.sh --db=$DB   （或不传 --db，默认 mysql）"
    exit 0
  fi
  sleep 1
done

error "90s 内未全部健康： $DB=$dbh redis=$rd"
docker compose -f "$COMPOSE_FILE" --profile "$DB" ps
exit 1
