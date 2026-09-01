> **⚠️ 部分内容已过期（2026-09-01）**：本仓库已拿掉多租户机制，本文 Phase 1（租户管理）
> 与 Phase 8（默认租户与登录模式）已随去多租户化改造整体删除，不再是本平台的功能范围。
> 其余 Phase（部门/菜单/角色/用户/字典/日志管理）均已完成实现（去掉 tenant_id 维度）。
> 原因见 `doc/design/modules/core/去多租户化-概要设计.md`。

# 实现计划：RBAC 完整实现

## 概述

按 Phase 1-8 的依赖顺序，逐模块实现 RBAC 剩余功能。每个 Phase 内先后端再前端，后端按 Entity → Mapper → Service → Controller 顺序，前端按 API 服务 → 页面组件 → 路由集成顺序。Phase 9 做全量编译验证和路由整合。

所有代码严格按照 `doc/design/modules/rbac/modules/` 下各子模块的详细设计文档实现。

## 任务

- [ ] 1. Phase 1：租户管理（后端 CRUD 完善 + 前端页面）
  - [x] 1.1 后端：创建 TenantService 接口和 TenantServiceImpl 实现类
    - 实现 7 个接口方法：list、getDetail、create（含编排逻辑：创建默认部门+管理员角色+admin用户+分配所有菜单）、update、remove、updateConfig、updateStatus
    - 创建 DTO：TenantCreateDTO、TenantUpdateDTO、TenantQueryDTO、TenantConfigDTO、TenantStatusDTO
    - 创建 VO：TenantVO、TenantListVO、TenantDetailVO、TenantCreateResultVO、TenantStatistics
    - 新增租户编排逻辑在同一事务内，失败全部回滚
    - 禁用租户时踢出该租户所有在线用户会话
    - 删除租户校验仅允许存在 admin 用户
    - _Requirements: 1.2, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12_

  - [x] 1.2 后端：完善 TenantController，添加 TENANT-001 至 TENANT-007 全部接口
    - 补全 list、detail、create、update、remove、config、status 7 个端点
    - 所有写接口添加 @Log 注解
    - 配置权限标识：system:tenant:list / add / edit / remove / config
    - 全局表需手动处理权限（sys_tenant 无 tenant_id，不受租户拦截器过滤）
    - _Requirements: 1.12_

  - [x] 1.3 后端：TenantMapper 补全查询方法
    - 补全 selectOptions（已有）、selectByCode、分页列表查询（含 userCount 统计）、详情查询（含 statistics）
    - _Requirements: 1.1, 1.5, 1.12_

  - [ ]* 1.4 后端：编写租户管理单元测试
    - 测试新增租户编排事务完整性（Property 1）
    - 测试租户编码唯一性（Property 2）
    - 测试租户编码不可修改（Property 3）
    - 测试禁用租户踢出会话（Property 10）
    - 测试删除租户前置约束（Property 6d）
    - **Property 1: 租户编排事务完整性**
    - **Property 2: 实体唯一性约束（租户编码）**
    - **Validates: Requirements 1.2, 1.7, 1.8, 1.9**

  - [x] 1.5 前端：创建 tenantApi 服务（`frontend/src/services/tenantApi.ts`）
    - 实现 TENANT-001 至 TENANT-007 全部接口调用
    - 定义 TypeScript 类型：TenantListVO、TenantDetailVO、TenantCreateResultVO、TenantConfigDTO 等
    - _Requirements: 1.12, 1.13_

  - [x] 1.6 前端：创建 TenantPage 及弹窗组件
    - TenantPage：搜索栏 + 表格 + 分页，按权限控制按钮显隐
    - TenantFormModal：新增/编辑共用弹窗，编辑模式租户编码禁用
    - TenantDetailModal：详情弹窗，Descriptions 展示含统计数据
    - TenantConfigModal：配置弹窗，功能开关 + 限额 + 地图服务商
    - PasswordShowModal：新增成功后展示管理员初始密码，提示仅此一次
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6, 1.13_

  - [x] 1.7 前端：App.tsx 添加租户管理路由 `/system/tenants`
    - _Requirements: 1.13_

- [ ] 2. Phase 1 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 3. Phase 2：部门管理（后端 + 前端）
  - [x] 3.1 后端：创建 Dept Entity（`com.gentry.rbac.dept.entity.Dept`）
    - 继承 TenantEntity，映射 sys_dept 表
    - 字段：id、parentId、ancestors、name、leaderId、leaderName、phone、email、sort、status
    - _Requirements: 2.8_

  - [x] 3.2 后端：创建 DeptMapper 接口
    - selectList（按 name/status 过滤）、selectById、insert、update、logicDeleteById
    - countByParentId（统计子部门数）
    - selectByAncestorsLike（查询子孙部门）
    - _Requirements: 2.8_

  - [x] 3.3 后端：创建 DeptService 接口和 DeptServiceImpl
    - tree（构建树形结构 + 填充 userCount）、getDetail、create（校验名称唯一+层级≤5+计算ancestors）、update（校验不能移动到子部门下+递归更新子孙ancestors）、remove（校验无用户无子部门）
    - 创建 DTO：DeptCreateDTO、DeptUpdateDTO、DeptQueryDTO
    - 创建 VO：DeptVO、DeptTreeVO
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.8_

  - [x] 3.4 后端：创建 DeptController（5 个接口 API-001 至 API-005）
    - 所有写接口添加 @Log 注解
    - 配置权限标识：system:dept:list / add / edit / remove
    - _Requirements: 2.8_

  - [ ]* 3.5 后端：编写部门管理单元测试
    - 测试 ancestors 一致性（Property 4）
    - 测试删除前置约束（Property 6a）
    - 测试部门名称唯一性（Property 2）
    - **Property 4: 部门树 ancestors 一致性**
    - **Property 6: 删除前置约束（部门）**
    - **Validates: Requirements 2.2, 2.3, 2.4**

  - [x] 3.6 前端：创建 deptApi 服务（`frontend/src/services/deptApi.ts`）
    - 实现 DEPT-001 至 DEPT-005 全部接口调用
    - _Requirements: 2.9_

  - [x] 3.7 前端：创建 DeptPage 及组件
    - DeptPage：搜索栏 + 树形表格（默认展开全部）+ 展开/折叠按钮
    - DeptFormModal：新增/编辑弹窗，集成 DeptTreeSelect 选择上级部门
    - DeptTreeSelect 公共组件：受控模式、允许清空、排除指定部门、TanStack Query 缓存 10min
    - _Requirements: 2.1, 2.7, 2.9_

  - [x] 3.8 前端：App.tsx 添加部门管理路由 `/system/dept`
    - _Requirements: 2.9_

- [ ] 4. Phase 2 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 5. Phase 3：菜单管理（后端 + 前端）
  - [x] 5.1 后端：确认 Menu Entity，补全字段映射
    - 全局表（无 tenant_id），继承 BaseEntity
    - 确认字段：id、parentId、name、icon、type、sort、permission、path、component、visible、status、isExternal、isCache、active、query
    - _Requirements: 3.5, 3.8_

  - [x] 5.2 后端：创建/完善 MenuMapper
    - selectList（按 name/status/type 过滤）、selectById、insert、update、batchLogicDelete
    - _Requirements: 3.8_

  - [x] 5.3 后端：创建 MenuService 接口和 MenuServiceImpl
    - tree（构建三级树）、getDetail、create（按 type 动态校验必填字段+同级名称唯一）、update（type 不可修改+不能移动到子菜单下）、remove（递归删除子菜单+清除 sys_role_menu 关联）
    - 创建 DTO：MenuCreateDTO、MenuUpdateDTO、MenuQueryDTO
    - 创建 VO：MenuVO、MenuTreeVO
    - 创建枚举：MenuType（DIR=1, MENU=2, BUTTON=3）
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.8_

  - [x] 5.4 后端：创建 MenuController（5 个接口 API-001 至 API-005）
    - 所有写接口添加 @Log 注解
    - 配置权限标识：system:menu:list / add / edit / remove
    - _Requirements: 3.8_

  - [ ]* 5.5 后端：编写菜单管理单元测试
    - 测试级联删除完整性（Property 5 菜单部分）
    - 测试不可变字段保护（Property 3 菜单类型）
    - **Property 5: 级联删除完整性（菜单）**
    - **Property 3: 不可变字段保护（菜单类型）**
    - **Validates: Requirements 3.3, 3.4**

  - [x] 5.6 前端：完善 menuApi 服务（`frontend/src/services/menuApi.ts`）
    - 实现 MENU-001 至 MENU-005 全部接口调用
    - _Requirements: 3.9_

  - [x] 5.7 前端：创建 MenuPage 及组件
    - MenuPage：搜索栏（名称+状态+类型）+ 树形表格（不分页，默认展开全部）+ 展开/折叠按钮
    - MenuFormModal：新增/编辑弹窗，根据 type 动态显隐表单字段，编辑模式 type 禁用
    - IconPicker：Ant Design 图标网格选择器，支持搜索过滤
    - _Requirements: 3.1, 3.6, 3.7, 3.9_

  - [x] 5.8 前端：App.tsx 添加菜单管理路由 `/system/menu`
    - _Requirements: 3.9_

- [ ] 6. Phase 3 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 7. Phase 4：角色管理（后端 + 前端）
  - [x] 7.1 后端：确认 Role/RoleMenu/RoleDept Entity，补全字段映射
    - Role 继承 TenantEntity，RoleMenu/RoleDept 为全局关联表
    - 创建 DataScope 枚举（ALL=1, DEPT_AND_CHILD=2, DEPT_ONLY=3, SELF_ONLY=4, CUSTOM=5）
    - _Requirements: 4.10_

  - [x] 7.2 后端：创建/完善 RoleMapper、RoleMenuMapper、RoleDeptMapper
    - RoleMapper：分页列表（含 userCount 统计）、详情（含 menuIds/deptIds）
    - RoleMenuMapper：deleteByRoleId、batchInsert、deleteByMenuIds
    - RoleDeptMapper：deleteByRoleId、batchInsert
    - _Requirements: 4.10_

  - [x] 7.3 后端：创建 RoleService 接口和 RoleServiceImpl
    - list、getDetail、create（编码唯一+禁止内置编码）、update（编码不可修改）、remove（非内置+无关联用户+事务内逻辑删除角色+物理删除关联）、assignMenus（先删后插）、updateDataScope（CUSTOM 时校验 deptIds 非空）、listUsers、updateStatus
    - 创建 DTO：RoleCreateDTO、RoleUpdateDTO、RoleQueryDTO、RoleMenuAssignDTO、RoleDataScopeDTO
    - 创建 VO：RoleVO、RoleListVO、RoleDetailVO
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.10_

  - [x] 7.4 后端：创建 RoleController（9 个接口 API-001 至 API-009）
    - 所有写接口添加 @Log 注解
    - 配置权限标识：system:role:list / add / edit / remove / assignMenu / assignDataScope
    - _Requirements: 4.10_

  - [ ]* 7.5 后端：编写角色管理单元测试
    - 测试权限分配先删后插一致性（Property 7 角色菜单部分）
    - 测试删除前置约束（Property 6b）
    - 测试不可变字段保护（Property 3 角色编码）
    - **Property 7: 权限分配先删后插一致性（角色菜单）**
    - **Property 6: 删除前置约束（角色）**
    - **Validates: Requirements 4.2, 4.4, 4.7**

  - [x] 7.6 前端：创建 roleApi 服务（`frontend/src/services/roleApi.ts`）
    - 实现 ROLE-001 至 ROLE-009 全部接口调用
    - _Requirements: 4.11_

  - [x] 7.7 前端：创建 RolePage 及组件
    - RolePage：搜索栏 + 角色表格（分页）+ 新增/编辑弹窗 + 状态 Switch
    - RoleFormModal：新增/编辑弹窗，编辑模式角色编码禁用
    - _Requirements: 4.1, 4.2, 4.3, 4.11_

  - [x] 7.8 前端：创建 PermissionPage（角色权限分配页）
    - 路由 `/system/roles/:id/permissions`，初始化并行加载角色详情+菜单树+部门树
    - MenuTree：菜单权限树（可勾选、全选、清空），提交时合并 checkedKeys + halfCheckedKeys
    - DataScopeCard：5 级数据权限 Radio + 自定义部门树多选（DeptTreeCheck）
    - 保存时并行调用 ROLE-006 + ROLE-007
    - _Requirements: 4.4, 4.5, 4.6, 4.9, 4.11_

  - [x] 7.9 前端：App.tsx 添加角色管理路由 `/system/roles` 和 `/system/roles/:id/permissions`
    - _Requirements: 4.11_

- [ ] 8. Phase 4 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 9. Phase 5：用户管理完善（后端补全 + 前端完善）
  - [ ] 9.1 后端：UserService 补全和确认
    - 确认 USER-001 至 USER-008 和 AUTH-001 至 AUTH-004 全部实现
    - 补全 deptId 筛选逻辑：通过 DeptService.getChildDeptIds 查询子部门 ID 列表，用 IN 条件过滤
    - 补全密码强度校验（8-20位，含大小写字母+数字）
    - 补全删除用户后踢出在线会话
    - 补全重置密码后踢出在线会话
    - _Requirements: 5.2, 5.3, 5.6, 5.7, 5.10_

  - [ ] 9.2 后端：UserController 确认所有接口和权限标识
    - 确认所有写接口添加 @Log 注解
    - 确认权限标识：system:user:list / add / edit / remove / resetPwd / assignRole
    - _Requirements: 5.10_

  - [ ]* 9.3 后端：编写用户管理单元测试
    - 测试密码强度校验（Property 11）
    - 测试权限分配先删后插一致性（Property 7 用户角色部分）
    - 测试删除前置约束（Property 6c）
    - 测试部门筛选包含子部门（Property 15）
    - **Property 11: 密码强度校验**
    - **Property 7: 权限分配先删后插一致性（用户角色）**
    - **Property 15: 部门筛选用户包含子部门**
    - **Validates: Requirements 5.2, 5.3, 5.5, 5.7**

  - [ ] 9.4 前端：UserPage 改造为左树右表布局
    - 左侧：DeptTreeSelect 组件（部门管理模块提供），点击节点以 deptId 筛选用户列表
    - 右侧：用户列表表格 + 搜索栏 + 操作按钮
    - _Requirements: 5.1, 5.2, 5.11_

  - [ ] 9.5 前端：UserFormModal 集成 DeptTreeSelect 和角色选择
    - 新增表单集成：DeptTreeSelect（所属部门）、角色多选（角色列表接口）、密码强度校验
    - 编辑表单：用户名禁用，支持修改部门、角色、职务等
    - _Requirements: 5.3, 5.4, 5.11_

  - [ ] 9.6 前端：确认 RoleAssignModal 和 PasswordResetModal 功能完整
    - RoleAssignModal：预选用户当前角色，提交先删后插
    - PasswordResetModal：密码强度校验，重置成功提示
    - _Requirements: 5.5, 5.6, 5.11_

- [ ] 10. Phase 5 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 11. Phase 6：字典管理（后端 + 前端）
  - [ ] 11.1 后端：创建 DictType 和 DictData Entity
    - DictType 继承 TenantEntity，映射 sys_dict_type
    - DictData 继承 TenantEntity，映射 sys_dict_data
    - _Requirements: 6.9_

  - [ ] 11.2 后端：创建 DictTypeMapper 和 DictDataMapper
    - DictTypeMapper：分页列表（含 dataCount 统计）、selectByDictType
    - DictDataMapper：selectByDictType（按 sort 排序）、insert、update、logicDeleteByDictType
    - _Requirements: 6.9_

  - [ ] 11.3 后端：创建 DictService 接口和 DictServiceImpl
    - listTypes、createType（编码唯一）、updateType（编码不可修改）、removeType（级联删除数据项+清缓存）
    - listDataByType（Caffeine 缓存）、createData（键值唯一+类型存在）、updateData、removeData
    - refreshCache（清除所有缓存）
    - 缓存键：`dict:{tenantId}:{dictType}`，写操作后自动清除相关缓存
    - 创建 DTO：DictTypeCreateDTO、DictTypeUpdateDTO、DictTypeQueryDTO、DictDataCreateDTO、DictDataUpdateDTO
    - 创建 VO：DictTypeVO、DictTypeListVO、DictDataVO
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.9_

  - [ ] 11.4 后端：创建 DictController（9 个接口 DICT-001 至 DICT-009）
    - 所有写接口添加 @Log 注解
    - 配置权限标识：system:dict:list / add / edit / remove
    - _Requirements: 6.9_

  - [ ] 11.5 后端：配置 Caffeine 缓存
    - 添加 Caffeine 依赖（如未添加）
    - 配置字典数据缓存 Bean
    - _Requirements: 6.9_

  - [ ]* 11.6 后端：编写字典管理单元测试
    - 测试级联删除完整性（Property 5 字典部分）
    - 测试实体唯一性约束（Property 2 字典类型编码+字典键值）
    - 测试不可变字段保护（Property 3 字典类型编码）
    - **Property 5: 级联删除完整性（字典）**
    - **Property 2: 实体唯一性约束（字典）**
    - **Validates: Requirements 6.3, 6.4, 6.5**

  - [ ] 11.7 前端：创建 dictApi 服务（`frontend/src/services/dictApi.ts`）
    - 实现 DICT-001 至 DICT-009 全部接口调用
    - _Requirements: 6.10_

  - [ ] 11.8 前端：创建 DictPage（双视图切换）及弹窗组件
    - DictPage：通过 activeDict 状态控制显示"类型列表"或"数据详情"
    - 类型列表视图：搜索栏 + 类型表格（分页）+ 刷新缓存按钮
    - 数据详情视图：返回列表按钮 + 类型信息栏 + 数据项表格（不分页，按 sort 排序）
    - TypeFormModal：新增/编辑字典类型弹窗，编辑模式 dictType 禁用
    - DataFormModal：新增/编辑字典数据弹窗，编辑模式 dictValue 禁用
    - _Requirements: 6.1, 6.2, 6.6, 6.10_

  - [ ] 11.9 前端：创建 DictSelect 和 DictTag 通用组件
    - DictSelect：按 dictType 渲染 Select 下拉框，TanStack Query 缓存 30min，仅展示启用数据项
    - DictTag：按 cssClass 渲染 Tag（primary→蓝、success→绿、warning→橙、danger→红、default→灰）
    - _Requirements: 6.7, 6.8, 6.10_

  - [ ] 11.10 前端：App.tsx 添加字典管理路由 `/system/dict`
    - _Requirements: 6.10_

- [ ] 12. Phase 6 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 13. Phase 7：日志管理（后端 + 前端）
  - [ ] 13.1 后端：创建 OperLog 和 LoginLog Entity
    - OperLog 映射 sys_oper_log（含 tenant_id，无 deleted 字段，物理删除）
    - LoginLog 映射 sys_login_log（含 tenant_id，无 deleted 字段，物理删除）
    - _Requirements: 7.10_

  - [ ] 13.2 后端：创建 OperLogMapper 和 LoginLogMapper
    - OperLogMapper：分页列表（按 operate_time DESC）、详情、物理删除（按时间范围）
    - LoginLogMapper：分页列表（按 login_time DESC）、详情、物理删除（按时间范围）
    - _Requirements: 7.10_

  - [ ] 13.3 后端：创建 LogService 接口和 LogServiceImpl
    - 操作日志：listOperLogs、getOperLogDetail、cleanOperLogs（beforeDays≥30）、saveOperLog
    - 登录日志：listLoginLogs、getLoginLogDetail、cleanLoginLogs（beforeDays≥30）、saveLoginLog（解析 User-Agent）
    - 创建 DTO：OperLogQueryDTO、LoginLogQueryDTO、LogCleanDTO
    - 创建 VO：OperLogListVO、OperLogDetailVO、LoginLogListVO、LoginLogDetailVO、OnlineUserVO
    - _Requirements: 7.1, 7.2, 7.6, 7.7, 7.8, 7.9, 7.10_

  - [ ] 13.4 后端：创建 @Log 注解和 LogAspect AOP 切面
    - @Log 注解：module、type、title、saveResult 属性
    - LogAspect：拦截 @Log 标注方法，记录开始时间、解析注解参数、获取操作人信息、序列化请求参数（脱敏 password/oldPassword/newPassword）、计算耗时、记录状态
    - 日志记录失败不影响业务（catch 后仅记录 ERROR 日志）
    - _Requirements: 7.7, 7.9, 7.10_

  - [ ] 13.5 后端：在 AuthServiceImpl 中集成登录日志记录
    - 登录成功时调用 LogService.saveLoginLog（status=1, tenantId=用户租户ID）
    - 登录失败时调用 LogService.saveLoginLog（status=0, tenantId=0）
    - 解析 User-Agent 获取 browser、os、deviceType
    - _Requirements: 7.8, 7.10_

  - [ ] 13.6 后端：创建 LogController 和 OnlineController
    - LogController：LOG-001 至 LOG-008（操作日志+登录日志的列表、详情、清理、导出）
    - OnlineController：LOG-009 在线用户列表（Sa-Token Session 查询）、LOG-010 强制下线（校验不能下线自己）
    - 配置权限标识：monitor:operlog:* / monitor:loginlog:* / monitor:online:*
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.10_

  - [ ] 13.7 后端：为所有已有 Controller 的写接口补充 @Log 注解
    - TenantController、DeptController、MenuController、RoleController、UserController 的所有写接口
    - _Requirements: 7.7_

  - [ ]* 13.8 后端：编写日志管理单元测试
    - 测试操作日志自动采集与脱敏（Property 12）
    - 测试登录日志完整记录（Property 13）
    - 测试日志清理最小保留期（Property 14）
    - **Property 12: 操作日志自动采集与脱敏**
    - **Property 13: 登录日志完整记录**
    - **Property 14: 日志清理最小保留期**
    - **Validates: Requirements 7.6, 7.7, 7.8**

  - [ ] 13.9 前端：创建 logApi 服务（`frontend/src/services/logApi.ts`）
    - 实现 LOG-001 至 LOG-010 全部接口调用
    - _Requirements: 7.11_

  - [ ] 13.10 前端：创建 OperLogPage（操作日志页）
    - 搜索栏（操作用户+模块+IP+结果+时间范围）+ 表格（分页）+ 清空/导出按钮
    - OperLogDetail Drawer：展示请求参数 JSON、响应结果 JSON、错误信息、耗时
    - _Requirements: 7.1, 7.11_

  - [ ] 13.11 前端：创建 LoginLogPage（登录日志页）
    - 搜索栏（用户名+状态+时间范围）+ 表格（分页）+ 清理/导出/刷新按钮
    - LoginLogDetail Drawer：展示登录信息 + 设备信息（deviceType、User-Agent）
    - _Requirements: 7.2, 7.11_

  - [ ] 13.12 前端：创建 OnlineUserPage（在线用户页）
    - 搜索栏（用户名）+ 在线人数 Badge + 表格（不分页）+ 强退按钮（二次确认）
    - _Requirements: 7.3, 7.4, 7.5, 7.11_

  - [ ] 13.13 前端：App.tsx 添加日志管理路由
    - `/logs/system`（操作日志）、`/logs/login`（登录日志）、`/logs/online`（在线用户）
    - _Requirements: 7.11_

- [ ] 14. Phase 7 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 15. Phase 8：默认租户与登录模式联调
  - [ ] 15.1 后端：创建 TenantConstants 常量类
    - `DEFAULT_TENANT_ID = 1L`
    - 放置在 `com.gentry.core.constant` 包下
    - _Requirements: 8.10_

  - [ ] 15.2 后端：修改 LoginDTO，tenantId 替换为 tenantCode
    - tenantCode: String 类型，可选，@Size(max=50)
    - 移除原 tenantId 字段
    - _Requirements: 8.9, 8.10_

  - [ ] 15.3 后端：重构 AuthServiceImpl.login() 方法
    - 抽取三个私有方法：resolveTenantId()、findAndValidateUser()、buildLoginResult()
    - resolveTenantId：tenantCode 为空→DEFAULT_TENANT_ID（不查库不校验），不为空→查 sys_tenant 校验存在性+状态+过期
    - login() 圈复杂度降为 1
    - _Requirements: 8.2, 8.3, 8.8, 8.10_

  - [ ] 15.4 后端：TenantMapper 补全 selectByCode 方法
    - `SELECT * FROM sys_tenant WHERE code = ? AND deleted = 0`
    - _Requirements: 8.10_

  - [ ]* 15.5 后端：编写登录模式单元测试
    - 测试登录租户解析（Property 9）
    - **Property 9: 登录租户解析**
    - **Validates: Requirements 8.2, 8.3**

  - [ ] 15.6 前端：LoginPage 改造支持 Tabs 切换
    - 默认登录 Tab：仅用户名+密码
    - 租户登录 Tab：租户下拉框（TENANT-009）+ 用户名 + 密码
    - 租户下拉框：头部插入"默认租户"（value=""），接口失败仅保留默认租户选项
    - 提交逻辑：默认登录不传 tenantCode，租户登录选中默认租户不传 tenantCode，选中具体租户传 tenantCode
    - _Requirements: 8.1, 8.4, 8.11_

  - [ ] 15.7 前端：更新 LoginDTO 类型定义
    - 移除 tenantId，添加可选 tenantCode: string
    - 更新 authApi.login 调用
    - _Requirements: 8.9, 8.11_

- [ ] 16. Phase 8 检查点
  - 确保所有测试通过，询问用户是否有问题。

- [ ] 17. Phase 9：全量编译验证 + 路由整合
  - [ ] 17.1 后端全量编译验证
    - 执行 `mvn clean compile` 确保无编译错误
    - 检查所有模块间依赖正确

  - [ ] 17.2 前端全量编译验证
    - 执行 `npm run build` 确保无 TypeScript 编译错误
    - 检查所有 import 路径正确

  - [ ] 17.3 App.tsx 路由整合确认
    - 确认所有新页面路由已注册：/system/tenants、/system/dept、/system/menu、/system/roles、/system/roles/:id/permissions、/system/users、/system/dict、/logs/system、/logs/login、/logs/online
    - 确认 ProtectedRoute 包裹所有需认证的路由

  - [ ] 17.4 AppSidebar 菜单更新
    - 确认侧边栏菜单结构与路由一致
    - 系统管理：租户管理、用户管理、角色管理、菜单管理、部门管理、字典管理
    - 日志管理：操作日志、登录日志、在线用户

- [ ] 18. 最终检查点
  - 确保所有测试通过，询问用户是否有问题。

## 备注

- 任务标记 `*` 为可选测试任务，可跳过以加速 MVP
- 每个任务引用具体需求编号，确保需求全覆盖
- 检查点确保增量验证，及时发现问题
- 属性测试验证跨所有输入的通用正确性属性
- 后端使用 MyBatis-Flex（非 MyBatis-Plus），注意 API 差异
- 前端使用 TanStack Query 管理接口缓存，写操作后 invalidateQueries 刷新
