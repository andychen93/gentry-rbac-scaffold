# CLAUDE.md

本仓库的技术宪法在 **[AGENTS.md](./AGENTS.md)**，请先完整读一遍再动手。

配套文档索引在 **[DOC_INDEX.md](./DOC_INDEX.md)**。

## 快速定位

| 我要… | 去哪 |
|-------|------|
| 加一个业务模块（后端） | `doc/guide/RBAC模块开发指南.md`，照抄 `com.precision.rbac.dept` |
| 用横切组件（限流/数据权限/日志…） | `doc/guide/Core组件开发指南.md` |
| 加一个页面（前端） | `frontend/src/pages/dept/` 是最简样例；页面必须在 `utils/menuMapper.ts` 登记 |
| 加权限点 | 写 Flyway 迁移插 `sys_menu` + `sys_role_menu`，权限串与 `@SaCheckPermission` 一致 |
| 改数据库 | 只能新增 `backend/precision-start/src/main/resources/db/migration/V{n}__xxx.sql` |
| 改配色 / 主题 | `frontend/src/theme/argonColors.ts`，对照页 `/dev/style` |
| 起本地环境 | `bash scripts/deps_up.sh && bash scripts/dev_up.sh` |

## 硬约束（违反即 Review 打回）

1. 不写死 `tenant_id`，一律 `UserContext.getTenantId()`
2. 不 `throw new RuntimeException`，一律 `BizException(ErrorCode.XXX)`
3. Controller 不直接调 Mapper
4. 不修改已发布的 Flyway 迁移文件
5. 无详细设计文档不编码
6. 提交前 `mvn test` + `npm test` + `npx tsc -b` 全绿
