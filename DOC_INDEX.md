# 文档索引

## 必读

| 文档 | 说明 |
|------|------|
| [AGENTS.md](./AGENTS.md) | 技术宪法。强制约定，唯一权威，动手前完整读一遍 |
| [README.md](./README.md) | 五分钟跑起来 + 怎么加业务模块 + 上生产清单 |
| [doc/standards/CCF微服务开发白皮书_v2.2.md](./doc/standards/CCF微服务开发白皮书_v2.2.md) | 开发流程标准（文档先行、评审门禁） |

## 开发指南

| 文档 | 说明 |
|------|------|
| [doc/guide/RBAC模块开发指南.md](./doc/guide/RBAC模块开发指南.md) | 业务模块从零到一：分层、命名、权限点、测试 |
| [doc/guide/Core组件开发指南.md](./doc/guide/Core组件开发指南.md) | 横切组件用法：数据权限、限流、防重、日志、TraceId |

## 架构与横切基础设施

| 文档 | 说明 |
|------|------|
| [doc/design/architecture/平台化改造概要设计.md](./doc/design/architecture/平台化改造概要设计.md) | starter 化总体设计：模块划分、自动装配、Flyway 双流、BOM、前端共享包 |
| [doc/design/architecture/全局基础设施架构设计.md](./doc/design/architecture/全局基础设施架构设计.md) | core-starter 整体设计（历史设计文档，内文沿用旧模块名） |
| `doc/design/architecture/全局基础设施架构图.drawio` | 架构图（drawio 打开） |
| `doc/design/modules/core/P0-*.md` | 全局异常、自动填充、StpInterface 权限接口 |
| `doc/design/modules/core/P1-*.md` | Jackson 全局配置、请求日志过滤器、数据权限拦截器 |
| `doc/design/modules/core/P2-*.md` | 全链路追踪、接口限流、重复提交防护、国际化 i18n（概要 + 前后端详细；主链路已落地。内置角色名见概要 §4.3；操作日志 module 列见 §4.8.1，title 仍中文） |

每个组件都有「概要设计 + 详细设计」两份。

## RBAC 模块

| 文档 | 说明 |
|------|------|
| [doc/design/modules/rbac/概要设计.md](./doc/design/modules/rbac/概要设计.md) | RBAC 整体设计（先读这个） |
| `doc/design/modules/rbac/RBAC模块架构图.drawio` | 模块架构图 |
| `doc/design/modules/rbac/SSO-JWT黑名单实现提示词.md` | JWT + 黑名单实现说明 |

功能级详细设计（每个功能一份后端 + 一份前端）：

| 功能 | 目录 |
|------|------|
| 用户管理 | `doc/design/modules/rbac/modules/用户管理/`（含「登录菜单动态渲染」） |
| 角色管理 | `doc/design/modules/rbac/modules/角色管理/` |
| 菜单管理 | `doc/design/modules/rbac/modules/菜单管理/` |
| 部门管理 | `doc/design/modules/rbac/modules/部门管理/` |
| 字典管理 | `doc/design/modules/rbac/modules/字典管理/` |
| 日志管理 | `doc/design/modules/rbac/modules/日志管理/` |
| 公共基础设施 | `doc/design/modules/rbac/modules/公共基础设施/`（双 Layout 布局、**前端权限体系**、前端国际化） |

### 公共基础设施详情

| 文档 | 说明 |
|------|------|
| `双Layout布局-前端详细设计.md` | 业务系统与系统管理两套 Layout 切换 |
| `前端权限体系-概要设计.md` | 权限体系总体思路（必读） |
| `前端权限体系-详细设计.md` | 权限实现规范、API 设计、最佳实践 |
| `前端权限体系-实现指南.md` | 逐步教程、代码示例、测试清单 |
| `前端权限体系-快速参考.md` | 一页纸查询表 |

## 运维监控

| 文档 | 说明 |
|------|------|
| `doc/design/modules/monitor/modules/Redis监控/后端详细设计.md` | Redis INFO / Key CRUD / 慢日志接口 |
| `doc/design/modules/monitor/modules/Redis监控/前端详细设计.md` | 监控页面设计 |
| `doc/design/modules/monitor/modules/Redis监控/Redis监控实现提示词.md` | 实现说明 |

## 需求文档（PRD）

| 文档 | 说明 |
|------|------|
| [doc/requirements/rbac/PRD-RBAC模块功能清单.md](./doc/requirements/rbac/PRD-RBAC模块功能清单.md) | 功能总清单（先读这个） |
| `doc/requirements/rbac/PRD-{用户,角色,菜单,部门,字典,日志}管理.md` | 各功能需求 |
| `doc/requirements/figma-make-prompt.md` | 产品用 Figma Make 出静态页的提示词模板 |

## 测试

| 文档 | 说明 |
|------|------|
| [doc/test/rbac/测试报告模板.md](./doc/test/rbac/测试报告模板.md) | 测试执行报告模板 |
| `doc/test/rbac/用户管理/用户管理-测试执行报告.md` | 报告样例（含截图） |

## Kiro Specs

| Spec | 说明 |
|------|------|
| `.kiro/specs/rbac-full-implementation/` | RBAC 全量实现的 requirements / design / tasks |
| `.kiro/specs/frontend-layout-system/` | 前端布局体系的 requirements / design / tasks |

## 参考资料

| 目录 | 说明 |
|------|------|
| `doc/reference/前端详细设计参考/` | 其他业务域的前端详细设计样板（设备、车辆、报警、报表、轨迹…），写新模块前端设计时当写作参考，不是本脚手架的功能 |
| [sql/README.md](./sql/README.md) | 数据库脚本说明：权威在 Flyway（`common/` + 三个厂商目录），`sql/reference/` 只是全量快照 |

---

## 一点说明

这些设计文档是从「车联网平台」项目沉淀下来的，少数文档的背景章节还留着「车联网平台」
的字样。技术内容与本脚手架代码一致，只有那部分业务背景描述需要你在派生项目里替换。
代码与 SQL 里的品牌字样已经统一改成中性名称（前端应用名在
`frontend/src/config/app.ts`）。
