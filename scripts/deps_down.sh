#!/usr/bin/env bash
# 停止本地开发依赖容器（数据卷保留；加 --purge 连数据一起删）
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/docker-compose.yml"

if [[ "${1:-}" == "--purge" ]]; then
  echo "[INFO] 停止容器并删除数据卷（数据库会被清空）"
  docker compose -f "$COMPOSE_FILE" down -v
else
  echo "[INFO] 停止容器，保留数据卷"
  docker compose -f "$COMPOSE_FILE" down
fi
echo "[ OK ] 完成"
