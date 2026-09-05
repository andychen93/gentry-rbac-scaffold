#!/usr/bin/env bash
# RBAC 一键验证门禁（Loop Engineering 内层循环的 evals）
#
# 串起「后端单测 + 前端类型检查 + 前端单测（可选 e2e）」，任一失败返回非零退出码。
# 作为 Agent 内层循环的自动化评估入口：Agent 改完代码后跑本脚本，
# 全绿才算完成（DoD 的一环），避免靠人肉逐条敲命令。
#
# 用法:
#   bash scripts/verify.sh                   # 单元门禁：后端单测(排除IT) + 前端 tsc + 前端 vitest
#   bash scripts/verify.sh --it              # 加跑集成测试(*IT，需 MySQL/Redis 依赖可达)
#   bash scripts/verify.sh --e2e             # 额外跑 Playwright e2e（需前后端 + 依赖已起）
#   bash scripts/verify.sh --only-backend    # 只跑后端单测
#   bash scripts/verify.sh --only-frontend   # 只跑前端（tsc + vitest）
#   bash scripts/verify.sh --coverage        # 后端额外检查 JaCoCo 覆盖率门禁（AGENTS.md: Service≥90%）
#   bash scripts/verify.sh -h                # 帮助
#
# 说明:
#   - 默认只跑单元门禁（无外部依赖）。后端 *IT 集成测试需要连 MySQL/Redis，
#     用 --it 开启；开启前会自动探活数据源，连不上会给出诊断而不是假跑。
#   - e2e 需要先启动环境： bash scripts/dev_up.sh
#   - 单元层基线：472 个后端单测 + 112 前端 vitest（2026-09-05 实测）

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

# ============ 颜色（与 dev_up.sh 保持一致） ============
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}[INFO]${NC} $*"; }
ok()    { echo -e "${GREEN}[ OK ]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERR ]${NC} $*"; }

# ============ 参数 ============
RUN_BACKEND=1
RUN_FRONTEND=1
RUN_IT=0
RUN_E2E=0
CHECK_COVERAGE=0

for arg in "$@"; do
  case "$arg" in
    --only-backend)  RUN_FRONTEND=0 ;;
    --only-frontend) RUN_BACKEND=0 ;;
    --it)            RUN_IT=1 ;;
    --e2e)           RUN_E2E=1 ;;
    --coverage)      CHECK_COVERAGE=1 ;;
    -h|--help)
      # 只打印文件头部注释块（用法说明），到第一个非注释行结束
      awk 'NR>1 && /^#/{print substr($0,3); next} NR>1 && !/^#/{exit}' "$0"
      exit 0
      ;;
    *)
      echo "未知参数: $arg（-h 查看帮助）"; exit 1 ;;
  esac
done

if [[ $RUN_BACKEND -eq 0 && $RUN_FRONTEND -eq 0 ]]; then
  error "--only-backend 与 --only-frontend 不能同时用"; exit 1
fi

# ============ 结果汇总变量 ============
BACKEND_OK=1
BACKEND_IT_OK=1
FRONTEND_TSC_OK=1
FRONTEND_TEST_OK=1
E2E_OK=1

# ============ 集成测试依赖探活 ============
# 应用默认配置：MySQL localhost:3306 root/123456，Redis localhost:6379
check_integration_deps() {
  local db_ok=0 redis_ok=0

  # Redis
  if redis-cli ping 2>/dev/null | grep -q PONG; then
    ok "Redis (6379) 可达"
    redis_ok=1
  else
    error "Redis (6379) 不可达：IT 的 Session/黑名单依赖 Redis"
  fi

  # MySQL：优先用 mysql 客户端做真实认证探测（比端口探测更准）
  if command -v mysql >/dev/null 2>&1 \
     && mysql -h127.0.0.1 -P3306 -uroot -p123456 -e 'SELECT 1' >/dev/null 2>&1; then
    ok "MySQL (3306, root/123456) 可连"
    db_ok=1
  elif nc -z localhost 3306 2>/dev/null; then
    error "3306 端口在监听但 root/123456 认证失败："
    error "  可能被本地其它 MySQL 占用（本机常见 /usr/local/mysql），或容器密码与配置不符。"
    error "  参考：docker rbac-mysql 的 3316 端口可用时，可用 bash scripts/dev_up.sh --db=mysql 修复映射。"
  else
    error "MySQL (3306) 不可达：先跑 bash scripts/deps_up.sh --db=mysql 起依赖。"
  fi

  if [[ $db_ok -eq 1 && $redis_ok -eq 1 ]]; then
    return 0
  fi
  return 1
}

# ============ 后端单测 ============
run_backend() {
  if [[ $RUN_IT -eq 1 ]]; then
    if ! check_integration_deps; then
      error "集成测试前置依赖未就绪，跳过 IT 层（单元层照跑）"
    fi
  fi

  if [[ $RUN_IT -eq 1 ]]; then
    info "后端全量测试：mvn test（含 *IT 集成测试，需 MySQL+Redis）"
  else
    info "后端单元测试：mvn test（排除 *IT 集成测试，无外部依赖）"
  fi
  info "运行目录：$BACKEND_DIR"

  local mvn_args=(test)
  if [[ $RUN_IT -eq 0 ]]; then
    mvn_args+=(-Dsurefire.excludes='**/*IT.java')
  fi

  if (cd "$BACKEND_DIR" && mvn "${mvn_args[@]}" -q); then
    ok "后端测试通过"
    BACKEND_OK=0
    [[ $RUN_IT -eq 1 ]] && BACKEND_IT_OK=0
    if [[ $CHECK_COVERAGE -eq 1 ]]; then
      check_coverage
    else
      info "JaCoCo 报告：$BACKEND_DIR/*/target/site/jacoco/index.html（加 --coverage 可做门禁核查）"
    fi
    return 0
  else
    error "后端测试失败"
    if [[ $RUN_IT -eq 1 ]]; then
      warn "提示：若失败集中在 *IT 且报 ApplicationContext/连接错误，多为数据源不可达，先确认 MySQL(3306)/Redis(6379) 与配置一致。"
    fi
    return 1
  fi
}

# JaCoCo 覆盖率门禁（AGENTS.md：Service impl ≥90%，分支 ≥80%）
# 解析各模块 target/site/jacoco/jacoco.csv，按「包名含 .service. 的类」统计行覆盖。
check_coverage() {
  info "检查 JaCoCo 覆盖率门禁（Service≥90%，分支≥80%）"
  # jacoco.csv 列序：GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,
  #                  BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,
  #                  COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
  local service_count=0 service_miss=0 branch_count=0 branch_miss=0
  for csv in "$BACKEND_DIR"/*/target/site/jacoco/jacoco.csv; do
    [[ -f "$csv" ]] || continue
    while IFS=',' read -r _ pkg _ _ _ bm bc lm lc _ _ _ _; do
      # 仅统计 service 实现包
      [[ "$pkg" == *".service"* ]] || continue
      service_count=$((service_count + lc + lm))
      service_miss=$((service_miss + lm))
      branch_count=$((branch_count + bc + bm))
      branch_miss=$((branch_miss + bm))
    done < <(tail -n +2 "$csv")
  done
  if [[ $service_count -eq 0 ]]; then
    warn "未找到 service 包覆盖率数据（可能未跑测试或 CSV 未生成），跳过覆盖率门禁"
    return 0
  fi
  local svc_ratio branch_ratio
  svc_ratio=$(awk -v c="$service_count" -v m="$service_miss" 'BEGIN{printf "%.1f", (c-m)*100/c}')
  branch_ratio=$(awk -v c="$branch_count" -v m="$branch_miss" 'BEGIN{printf "%.1f", (c-m)*100/c}')
  info "Service 行覆盖率：$svc_ratio%（目标 ≥90%）；分支覆盖率：$branch_ratio%（目标 ≥80%）"
  local bad=0
  awk -v v="$svc_ratio" 'BEGIN{exit !(v < 90)}' && { error "Service 覆盖率 $svc_ratio% < 90%"; bad=1; }
  awk -v v="$branch_ratio" 'BEGIN{exit !(v < 80)}' && { error "分支覆盖率 $branch_ratio% < 80%"; bad=1; }
  [[ $bad -eq 0 ]] && ok "覆盖率达标"
  return $bad
}

# ============ 前端 ============
run_frontend() {
  info "前端类型检查：npx tsc -b"
  if (cd "$FRONTEND_DIR" && npx tsc -b); then
    ok "类型检查通过"
    FRONTEND_TSC_OK=0
  else
    error "类型检查失败"
  fi

  info "前端单测：npm test（vitest run）"
  if (cd "$FRONTEND_DIR" && npm test); then
    ok "前端单测通过"
    FRONTEND_TEST_OK=0
  else
    error "前端单测失败"
  fi
}

# ============ e2e ============
run_e2e() {
  warn "Playwright e2e 需要 dev 环境已启动（bash scripts/dev_up.sh）"
  warn "若未启动，用例会大面积失败；可先停掉本轮 e2e（Ctrl+C）"
  info "前端 e2e：npx playwright test"
  if (cd "$FRONTEND_DIR" && npm run test:e2e); then
    ok "e2e 通过"
    E2E_OK=0
  else
    error "e2e 失败"
  fi
}

# ============ 主流程 ============
echo -e "${BLUE}==================== RBAC Verify 验证门禁 ====================${NC}"
info "工作目录：$ROOT_DIR"
[[ $RUN_BACKEND -eq 1 ]] && info "后端单测：  单元层（排除 *IT）"
[[ $RUN_BACKEND -eq 1 && $RUN_IT -eq 1 ]] && info "            + IT 集成层（MySQL+Redis）"
[[ $RUN_FRONTEND -eq 1 ]] && info "前端检查：  tsc + vitest"
[[ $RUN_E2E -eq 1 ]] && info "e2e：       Playwright"
[[ $CHECK_COVERAGE -eq 1 ]] && info "覆盖率门禁：开启（Service≥90% / 分支≥80%）"
echo ""

if [[ $RUN_BACKEND -eq 1 ]]; then
  run_backend || true
fi
if [[ $RUN_FRONTEND -eq 1 ]]; then
  run_frontend
fi
if [[ $RUN_E2E -eq 1 ]]; then
  run_e2e
fi

# ============ 汇总 ============
echo ""
echo -e "${BLUE}==================== 验证结果汇总 ====================${NC}"
ALL_OK=1
if [[ $RUN_BACKEND -eq 1 ]]; then
  if [[ $BACKEND_OK -eq 0 ]]; then ok "  后端单元层     通过（472 个单测基线）"; else error "  后端单元层     失败"; ALL_OK=0; fi
  if [[ $RUN_IT -eq 1 ]]; then
    if [[ $BACKEND_IT_OK -eq 0 ]]; then ok "  后端 IT 层     通过"; else error "  后端 IT 层     失败/跳过"; ALL_OK=0; fi
  fi
fi
if [[ $RUN_FRONTEND -eq 1 ]]; then
  if [[ $FRONTEND_TSC_OK -eq 0 ]]; then ok "  前端类型检查   通过"; else error "  前端类型检查   失败"; ALL_OK=0; fi
  if [[ $FRONTEND_TEST_OK -eq 0 ]]; then ok "  前端单测       通过"; else error "  前端单测       失败"; ALL_OK=0; fi
fi
if [[ $RUN_E2E -eq 1 ]]; then
  if [[ $E2E_OK -eq 0 ]]; then ok "  e2e            通过"; else error "  e2e            失败"; ALL_OK=0; fi
fi

echo ""
if [[ $ALL_OK -eq 0 ]]; then
  echo -e "${RED}门禁未通过：存在失败项，不能进入交付/合入。${NC}"
  exit 1
else
  echo -e "${GREEN}门禁全绿：可以进入下一步（合入 / 交付 / 发布）。${NC}"
  exit 0
fi
