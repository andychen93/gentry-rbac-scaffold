#!/usr/bin/env bash
# 用 Docker 启动本地开发依赖（PostgreSQL 16 + Redis 7）并等待健康
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/docker-compose.yml"

GREEN='\033[0;32m'; RED='\033[0;31m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC} $*"; }
error() { echo -e "${RED}[ERR ]${NC} $*"; }

if ! command -v docker >/dev/null 2>&1; then
  error "未安装 docker。可改为本机安装 PostgreSQL 16 + Redis 7，端口保持 5432 / 6379。"
  exit 1
fi

info "启动 PostgreSQL + Redis"
docker compose -f "$COMPOSE_FILE" up -d

info "等待健康检查……"
for i in $(seq 1 60); do
  pg=$(docker inspect -f '{{.State.Health.Status}}' rbac-postgres 2>/dev/null || echo starting)
  rd=$(docker inspect -f '{{.State.Health.Status}}' rbac-redis 2>/dev/null || echo starting)
  if [[ "$pg" == "healthy" && "$rd" == "healthy" ]]; then
    ok "PostgreSQL localhost:5432/precision"
    ok "Redis      localhost:6379"
    echo ""
    echo "下一步： bash scripts/dev_up.sh"
    exit 0
  fi
  sleep 1
done

error "60s 内未全部健康： postgres=$pg redis=$rd"
docker compose -f "$COMPOSE_FILE" ps
exit 1
