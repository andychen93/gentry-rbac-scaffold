#!/usr/bin/env bash
# 重建数据库，回到 Flyway 迁移定义的干净种子状态。
#
# 什么时候需要：
#   - E2E 报「ADMIN 权限已被改坏」（在界面上给 ADMIN 保存过全选权限）
#   - 手工点测积累了脏数据，想回到干净基线
#
# 用法:
#   bash scripts/db_reset.sh                 # 默认 mysql
#   bash scripts/db_reset.sh --db=postgresql
#   bash scripts/db_reset.sh --db=sqlite
#
# 库口令默认取 123456，可用环境变量覆盖：
#   DB_PASSWORD='xxx' bash scripts/db_reset.sh
#
# 重建后需要重启后端，Flyway 会在启动时重新执行全部迁移。
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

DB="mysql"
for arg in "$@"; do
  case "$arg" in
    --db=*) DB="${arg#--db=}" ;;
    -h|--help) grep -E '^# ' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "未知参数: $arg（-h 查看帮助）"; exit 1 ;;
  esac
done

DB_NAME="${DB_NAME:-precision}"
DB_PASSWORD="${DB_PASSWORD:-123456}"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

echo -e "${YELLOW}即将删除并重建数据库：$DB / $DB_NAME —— 库内所有数据都会丢失${NC}"
printf "确认继续？(y/N) "
read -r ans
[[ "$ans" == "y" || "$ans" == "Y" ]] || { echo "已取消"; exit 0; }

case "$DB" in
  mysql)
    MYSQL_USER="${DB_USER:-root}"
    mysql -h127.0.0.1 -P"${DB_PORT:-3306}" -u"$MYSQL_USER" -p"$DB_PASSWORD" \
      -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    ;;
  postgresql)
    PG_USER="${DB_USER:-postgres}"
    PGPASSWORD="$DB_PASSWORD" psql -h localhost -p "${DB_PORT:-5432}" -U "$PG_USER" \
      -c "DROP DATABASE IF EXISTS $DB_NAME;" -c "CREATE DATABASE $DB_NAME;"
    ;;
  sqlite)
    rm -f "$ROOT_DIR/backend/gentry-start/precision.db"
    echo "已删除 backend/gentry-start/precision.db"
    ;;
  *)
    echo -e "${RED}--db 只支持 mysql | postgresql | sqlite（当前: $DB）${NC}"; exit 1 ;;
esac

# Sa-Token 会话里缓存了旧的角色/权限，不清会话的话重启后老 Token 仍带着旧权限
if command -v redis-cli >/dev/null 2>&1 && redis-cli ping >/dev/null 2>&1; then
  redis-cli --scan --pattern 'Authorization:*' | xargs -r redis-cli del >/dev/null 2>&1 || true
  echo "已清理 Redis 里的 Sa-Token 会话缓存"
fi

echo -e "${GREEN}[ OK ]${NC} 数据库已重建"
echo "下一步：重启后端，Flyway 会自动重新执行全部迁移灌好种子数据"
