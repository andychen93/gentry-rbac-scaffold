#!/usr/bin/env bash
# 停止本地开发依赖容器（数据卷保留；加 --purge 连数据一起删）
#
# 用法:
#   bash scripts/deps_down.sh                    # 停掉当前起着的容器（不区分 db）
#   bash scripts/deps_down.sh --db=mysql          # 只停 mysql profile
#   bash scripts/deps_down.sh --purge             # 停 + 删数据卷
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/docker-compose.yml"

DB=""
PURGE=0
for arg in "$@"; do
  case "$arg" in
    --db=*) DB="${arg#--db=}" ;;
    --purge) PURGE=1 ;;
    -h|--help)
      grep -E '^# ' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "未知参数: $arg（-h 查看帮助）"; exit 1 ;;
  esac
done

# 未指定 --db 时对两个 profile 都发 down，docker compose 对未运行的 profile 是无操作
PROFILE_ARGS=(--profile mysql --profile postgresql)
if [[ -n "$DB" ]]; then
  PROFILE_ARGS=(--profile "$DB")
fi

if [[ $PURGE -eq 1 ]]; then
  echo "[INFO] 停止容器并删除数据卷（数据库会被清空）"
  docker compose -f "$COMPOSE_FILE" "${PROFILE_ARGS[@]}" down -v
else
  echo "[INFO] 停止容器，保留数据卷"
  docker compose -f "$COMPOSE_FILE" "${PROFILE_ARGS[@]}" down
fi
echo "[ OK ] 完成"
