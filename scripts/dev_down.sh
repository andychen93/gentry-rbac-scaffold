#!/usr/bin/env bash
# 停止 dev_up.sh 启动的前后端进程
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs/dev"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info() { echo -e "${YELLOW}[INFO]${NC} $*"; }
ok()   { echo -e "${GREEN}[ OK ]${NC} $*"; }

stop_pid() {
  local file=$1 name=$2
  if [[ -f "$file" ]]; then
    local pid
    pid=$(cat "$file")
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      info "停止 $name (PID=$pid)"
      kill "$pid" 2>/dev/null || true
      sleep 2
      if kill -0 "$pid" 2>/dev/null; then
        info "$name 未响应，强制终止"
        kill -9 "$pid" 2>/dev/null || true
      fi
      ok "$name 已停止"
    else
      info "$name 未在运行（PID=$pid）"
    fi
    rm -f "$file"
  else
    info "$name 无记录的 PID 文件"
  fi
}

stop_port() {
  local port=$1 name=$2
  local pids
  pids=$(lsof -ti ":$port" 2>/dev/null || true)
  if [[ -n "$pids" ]]; then
    info "$name (端口 $port) 仍被占用 PID=$pids，尝试清理"
    echo "$pids" | xargs kill -9 2>/dev/null || true
    ok "$name 端口已释放"
  fi
}

stop_pid "$BACKEND_PID_FILE"  "后端"
stop_pid "$FRONTEND_PID_FILE" "前端"

# 兜底清理可能残留的端口占用
stop_port 9090 "后端 HTTP"
stop_port 3030 "前端 Vite"

ok "全部清理完成"
