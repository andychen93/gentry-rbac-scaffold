# 需求文档

## 简介

本文档定义 RBAC（基于角色的访问控制）模块剩余功能的完整需求。项目当前已完成用户管理基础 CRUD 和登录认证，需按依赖关系依次完成租户管理、部门管理、菜单管理、角色管理、用户管理完善、字典管理、日志管理以及默认租户与登录模式联调共 8 个阶段的开发。

技术栈：后端 Spring Boot 3 + MyBatis-Flex + Sa-Token + PostgreSQL；前端 React 18 + Ant Design 5 + Zustand + React Router 6。所有代码严格按照 `doc/design/modules/rbac/modules/` 下各子模块的详细设计文档实现。

## 术语表

- **RBAC_System**：基于角色的访问控制系统，包含用户、角色、菜单、部门、租户、字典、日志等子模块
- **Tenant_Module**：租户管理模块，管理平台上的企业客户（租户），支持 CRUD、配置、状态切换，新增时自动编排创建默认部门、管理员角色和 admin 用户
- **Dept_Module**：部门管理模块，管理租户内多级组织架构树，支持 CRUD，提供 DeptTreeSelect 公共组件供其他模块复用
- **Menu_Module**：菜单管理模块，管理系统三级菜单树（目录 > 菜单 > 按钮），定义权限标识和前端路由映射，为角色权限分配提供数据源
- **Role_Module**：角色管理模块，管理角色定义、菜单权限分配（三级树勾选）、数据权限设置（5 级范围 + 自定义部门）
- **User_Module**：用户管理模块，管理租户内用户账号，支持 CRUD、角色分配、密码管理，提供认证登录能力
- **Dict_Module**：字典管理模块，管理系统枚举数据和配置项，提供两级字典管理（类型 + 数据项），支持 Caffeine 缓存
- **Log_Module**：日志管理模块，包含操作日志、登录日志和在线用户管理三个子功能
- **TenantLogin_Module**：默认租户与登录模式模块，支持默认登录（单租户）和租户登录（多租户）两种模式
- **TenantInterceptor**：多租户拦截器，自动为租户表 SQL 追加 tenant_id 条件
- **DataScopeInterceptor**：数据权限拦截器，根据角色的 data_scope 值动态追加 SQL 条件
- **Sa-Token**：Java 权限认证框架，提供 Token 管理、会话管理、权限校验能力
- **MyBatis-Flex**：ORM 框架，注意与 MyBatis-Plus 的 API 差异
- **BizException**：业务异常类，携带 ErrorCode 枚举，由 GlobalExceptionHandler 统一处理
- **DeptTreeSelect**：部门树选择器公共组件，被用户管理和角色管理模块复用
- **DictSelect**：字典下拉选择通用组件，按 dictType 渲染 Select 下拉框
- **DictTag**：字典标签通用组件，按 cssClass 渲染 Tag 标签

## 需求

### 需求 1：租户管理 CRUD（后端完善 + 前端页面）

**用户故事：** 作为平台超级管理员，我希望能够完整管理租户的生命周期（新增、编辑、查看、删除、配置、启用/禁用），以便高效管理平台上的企业客户。

#### 验收标准

1. WHEN 平台超级管理员访问租户管理页面, THE Tenant_Module SHALL 展示租户分页列表，支持按名称、编码、状态搜索筛选，表格包含编码、名称、联系人、用户数、到期时间、状态、创建时间等列
2. WHEN 平台超级管理员提交新增租户表单（含编码、名称、联系人、电话、邮箱、到期时间）, THE Tenant_Module SHALL 在同一事务内创建租户记录、默认部门、管理员角色（ADMIN，分配所有菜单权限）和 admin 用户（随机密码），并返回管理员初始密码
3. WHEN 新增租户成功后, THE Tenant_Module SHALL 弹窗展示管理员初始密码，明确提示"仅此一次展示，请妥善保管"，用户确认后关闭弹窗
4. WHEN 平台超级管理员编辑租户信息, THE Tenant_Module SHALL 允许修改名称、联系人、电话、邮箱、到期时间、备注，租户编码不可修改
5. WHEN 平台超级管理员查看租户详情, THE Tenant_Module SHALL 展示租户完整信息，包含统计数据（用户数、部门数、角色数）
6. WHEN 平台超级管理员配置租户参数, THE Tenant_Module SHALL 支持配置最大设备数、最大用户数、数据保留天数、视频/报警/报表功能开关、地图服务商
7. WHEN 平台超级管理员禁用租户, THE Tenant_Module SHALL 更新租户状态为禁用，并踢出该租户所有在线用户会话
8. WHEN 平台超级管理员删除租户, THE Tenant_Module SHALL 校验租户下无业务用户（仅允许存在 admin 用户），校验通过后逻辑删除租户
9. IF 新增租户时编码已存在, THEN THE Tenant_Module SHALL 返回错误提示"租户编码已存在"
10. IF 删除租户时租户下存在非 admin 用户, THEN THE Tenant_Module SHALL 返回错误提示"租户下存在用户，禁止删除"
11. THE Tenant_Module SHALL 对所有写操作添加 @Log 注解记录操作日志
12. WHEN 后端实现租户管理接口, THE Tenant_Module SHALL 严格按照 `doc/design/modules/rbac/modules/租户管理/后端详细设计.md` 中定义的 7 个接口（TENANT-001 至 TENANT-007）实现，包括入参校验、出参格式、错误码
13. WHEN 前端实现租户管理页面, THE Tenant_Module SHALL 严格按照 `doc/design/modules/rbac/modules/租户管理/前端详细设计.md` 实现，包括 TenantPage、SearchCard、TenantFormModal、TenantDetailModal、TenantConfigModal、PasswordShowModal 组件

### 需求 2：部门管理（后端 + 前端）

**用户故事：** 作为租户管理员，我希望能够管理租户内的多级组织架构树，以便为用户分配部门归属和实现数据权限控制。

#### 验收标准

1. WHEN 租户管理员访问部门管理页面, THE Dept_Module SHALL 以树形表格展示租户内所有部门层级，默认展开全部节点，支持按名称和状态搜索
2. WHEN 租户管理员新增部门（指定上级部门、名称、负责人、电话、邮箱、排序、状态）, THE Dept_Module SHALL 自动计算 ancestors 祖级路径，校验部门名称在租户内唯一，校验层级不超过 5 级
3. WHEN 租户管理员编辑部门, THE Dept_Module SHALL 校验不能将部门移动到自己的子部门下，移动时递归更新所有子孙节点的 ancestors
4. WHEN 租户管理员删除部门, THE Dept_Module SHALL 校验部门下无用户且无子部门，校验通过后逻辑删除
5. IF 部门下存在用户, THEN THE Dept_Module SHALL 返回错误提示"部门下存在用户，禁止删除"
6. IF 部门下存在子部门, THEN THE Dept_Module SHALL 返回错误提示"部门下存在子部门，禁止删除"
7. THE Dept_Module SHALL 提供 DeptTreeSelect 公共组件，支持受控模式、允许清空、排除指定部门（编辑时排除自身及子部门），使用 TanStack Query 缓存部门树数据（staleTime 10 分钟）
8. WHEN 后端实现部门管理接口, THE Dept_Module SHALL 严格按照 `doc/design/modules/rbac/modules/部门管理/后端详细设计.md` 中定义的 5 个接口（API-001 至 API-005）实现
9. WHEN 前端实现部门管理页面, THE Dept_Module SHALL 严格按照 `doc/design/modules/rbac/modules/部门管理/前端详细设计.md` 实现，包括 DeptPage、SearchCard、DeptTable、DeptFormModal、DeptTreeSelect 组件

### 需求 3：菜单管理（后端 + 前端）

**用户故事：** 作为平台超级管理员，我希望能够管理系统三级菜单树结构（目录 > 菜单 > 按钮），以便定义系统的导航结构和权限标识。

#### 验收标准

1. WHEN 平台超级管理员访问菜单管理页面, THE Menu_Module SHALL 以树形表格展示全部菜单数据（不分页），默认展开全部，支持按名称、状态、类型搜索
2. WHEN 平台超级管理员新增菜单, THE Menu_Module SHALL 根据菜单类型动态校验必填字段：目录需要名称；菜单需要名称、路由地址、组件路径；按钮需要名称和权限标识
3. WHEN 平台超级管理员编辑菜单, THE Menu_Module SHALL 禁止修改菜单类型（type），校验同级名称唯一，校验不能移动到自己的子菜单下
4. WHEN 平台超级管理员删除菜单, THE Menu_Module SHALL 递归删除所有子菜单，同时清除 sys_role_menu 中对应的角色-菜单关联记录
5. THE Menu_Module SHALL 为菜单表使用全局表设计（无 tenant_id），菜单数据对所有租户共享
6. WHEN 前端新增/编辑菜单弹窗中选择菜单类型, THE Menu_Module SHALL 动态显隐表单字段：目录显示名称、图标、路由、排序、状态；菜单显示全部字段；按钮仅显示名称、权限标识、排序、状态
7. THE Menu_Module SHALL 提供 IconPicker 图标选择器组件，支持 Ant Design 图标网格展示和搜索过滤
8. WHEN 后端实现菜单管理接口, THE Menu_Module SHALL 严格按照 `doc/design/modules/rbac/modules/菜单管理/后端详细设计.md` 中定义的 5 个接口（API-001 至 API-005）实现
9. WHEN 前端实现菜单管理页面, THE Menu_Module SHALL 严格按照 `doc/design/modules/rbac/modules/菜单管理/前端详细设计.md` 实现，包括 MenuPage、SearchBar、MenuTable、MenuFormModal、IconPicker 组件

### 需求 4：角色管理（后端 + 前端）

**用户故事：** 作为租户管理员，我希望能够管理角色定义、分配菜单权限和设置数据权限，以便实现精细化的权限控制。

#### 验收标准

1. WHEN 租户管理员访问角色列表页, THE Role_Module SHALL 展示角色分页列表，包含编码、名称、数据权限范围名称、关联用户数、排序、状态、创建时间，支持按名称、编码、状态搜索
2. WHEN 租户管理员新增角色（编码、名称、数据权限范围、排序、状态、备注）, THE Role_Module SHALL 校验角色编码在租户内唯一，编码格式为字母开头的 2-50 字符，禁止使用内置编码 ADMIN 和 USER
3. WHEN 租户管理员编辑角色, THE Role_Module SHALL 禁止修改角色编码
4. WHEN 租户管理员在角色权限页分配菜单权限, THE Role_Module SHALL 展示完整菜单权限树（可勾选），支持全选/清空快捷操作，提交时合并 checkedKeys 和 halfCheckedKeys 作为 menuIds，使用先删后插事务保证一致性
5. WHEN 租户管理员在角色权限页设置数据权限, THE Role_Module SHALL 支持 5 级数据权限范围（全部数据、本部门及子部门、本部门、仅本人、自定义），选择"自定义"时展示部门树多选
6. IF 数据权限范围为"自定义"且未选择部门, THEN THE Role_Module SHALL 阻止保存并提示"自定义数据权限必须选择部门"
7. WHEN 租户管理员删除角色, THE Role_Module SHALL 校验角色非内置（ADMIN/USER）且无关联用户，删除时在同一事务内逻辑删除角色、物理删除 sys_role_menu 和 sys_role_dept 关联
8. IF 角色下存在关联用户, THEN THE Role_Module SHALL 返回错误提示"角色下存在用户，禁止删除"
9. THE Role_Module SHALL 提供角色权限页（/system/roles/:id/permissions），初始化时并行加载角色详情、菜单树、部门树三个接口
10. WHEN 后端实现角色管理接口, THE Role_Module SHALL 严格按照 `doc/design/modules/rbac/modules/角色管理/后端详细设计.md` 中定义的 9 个接口（API-001 至 API-009）实现
11. WHEN 前端实现角色管理页面, THE Role_Module SHALL 严格按照 `doc/design/modules/rbac/modules/角色管理/前端详细设计.md` 实现，包括 RolePage、PermissionPage、MenuTree、DataScopeCard、DeptTreeCheck 组件

### 需求 5：用户管理完善（后端补全 + 前端完善）

**用户故事：** 作为租户管理员，我希望用户管理页面支持左树右表布局（部门树 + 用户列表），并完善角色分配、密码重置等功能，以便高效管理租户内用户。

#### 验收标准

1. WHEN 租户管理员访问用户管理页面, THE User_Module SHALL 采用左树右表布局，左侧展示部门树（使用 DeptTreeSelect 组件），右侧展示用户分页列表
2. WHEN 租户管理员点击部门树节点, THE User_Module SHALL 以该部门 ID 作为 deptId 参数查询用户列表（后端负责包含子部门用户）
3. WHEN 租户管理员新增用户, THE User_Module SHALL 在新增表单中集成部门选择（DeptTreeSelect）、角色多选（角色列表接口）、职务选择（字典 sys_user_post），密码字段使用强密码校验（8-20 位，含大小写字母+数字）
4. WHEN 租户管理员编辑用户, THE User_Module SHALL 禁止修改用户名，支持修改部门、角色、职务等字段
5. WHEN 租户管理员分配角色, THE User_Module SHALL 展示角色分配弹窗，预选用户当前角色，提交时使用先删后插事务
6. WHEN 租户管理员重置密码, THE User_Module SHALL 展示密码重置弹窗，校验新密码强度，重置成功后强制踢出该用户在线会话
7. WHEN 租户管理员删除用户, THE User_Module SHALL 校验不能删除当前登录用户和内置管理员（username=admin），删除后踢出该用户在线会话
8. IF 删除当前登录用户, THEN THE User_Module SHALL 返回错误提示"不能删除当前登录用户"
9. IF 删除 admin 用户, THEN THE User_Module SHALL 返回错误提示"系统管理员不可删除"
10. WHEN 后端补全用户管理功能, THE User_Module SHALL 确保所有 8 个用户管理接口（USER-001 至 USER-008）和 4 个认证接口（AUTH-001 至 AUTH-004）按 `doc/design/modules/rbac/modules/用户管理/后端详细设计.md` 完整实现
11. WHEN 前端完善用户管理页面, THE User_Module SHALL 严格按照 `doc/design/modules/rbac/modules/用户管理/前端详细设计.md` 实现左树右表布局和所有弹窗组件

### 需求 6：字典管理（后端 + 前端）

**用户故事：** 作为租户管理员，我希望能够管理系统中的枚举数据和配置项（字典类型 + 字典数据），以便统一维护下拉选项和标签展示。

#### 验收标准

1. WHEN 租户管理员访问字典管理页面, THE Dict_Module SHALL 展示字典类型分页列表，包含名称、类型编码、数据项数、状态、创建时间，支持按名称、编码、状态搜索
2. WHEN 租户管理员点击字典类型的"数据"操作, THE Dict_Module SHALL 在同一页面内切换到字典数据视图，展示该类型下所有数据项（不分页，按 sort 排序）
3. WHEN 租户管理员新增字典类型, THE Dict_Module SHALL 校验字典类型编码在租户内唯一，编码格式为字母开头的 2-100 字符
4. WHEN 租户管理员新增字典数据项, THE Dict_Module SHALL 校验字典键值在同一租户同一字典类型内唯一，校验对应字典类型存在且启用
5. WHEN 租户管理员删除字典类型, THE Dict_Module SHALL 在同一事务内逻辑删除字典类型和该类型下所有字典数据项，并清除对应 Caffeine 缓存
6. WHEN 租户管理员点击刷新缓存按钮, THE Dict_Module SHALL 调用后端接口清除所有字典 Caffeine 缓存
7. THE Dict_Module SHALL 提供 DictSelect 通用组件，按 dictType 渲染 Select 下拉框，使用 TanStack Query 缓存（staleTime 30 分钟），仅展示启用的数据项
8. THE Dict_Module SHALL 提供 DictTag 通用组件，按 cssClass 渲染 Ant Design Tag 标签（primary→蓝色、success→绿色、warning→橙色、danger→红色、default→灰色）
9. WHEN 后端实现字典管理接口, THE Dict_Module SHALL 严格按照 `doc/design/modules/rbac/modules/字典管理/后端详细设计.md` 中定义的 9 个接口（DICT-001 至 DICT-009）实现，包括 Caffeine 缓存策略
10. WHEN 前端实现字典管理页面, THE Dict_Module SHALL 严格按照 `doc/design/modules/rbac/modules/字典管理/前端详细设计.md` 实现，包括 DictPage（双视图切换）、TypeFormModal、DataFormModal、DictSelect、DictTag 组件

### 需求 7：日志管理（后端 + 前端）

**用户故事：** 作为租户管理员，我希望能够查看操作日志、登录日志和在线用户，以便进行审计追踪和安全监控。

#### 验收标准

1. WHEN 租户管理员访问操作日志页面, THE Log_Module SHALL 展示操作日志分页列表，支持按操作用户、模块、IP 地址、结果、时间范围搜索，点击详情展示 Drawer（含请求参数 JSON、响应结果 JSON、错误信息、耗时）
2. WHEN 租户管理员访问登录日志页面, THE Log_Module SHALL 展示登录日志分页列表，支持按用户名、状态、时间范围搜索，点击详情展示 Drawer（含设备信息、User-Agent）
3. WHEN 租户管理员访问在线用户页面, THE Log_Module SHALL 展示当前在线用户列表（不分页），显示在线人数 Badge，支持按用户名搜索
4. WHEN 管理员点击强制下线按钮, THE Log_Module SHALL 二次确认后调用 Sa-Token API 踢出指定用户会话，刷新在线用户列表
5. IF 管理员尝试强制下线自己, THEN THE Log_Module SHALL 返回错误提示"不能强制下线当前登录用户"
6. WHEN 管理员点击清理日志按钮, THE Log_Module SHALL 二次确认后物理删除指定天数之前的日志记录，最少保留 30 天
7. THE Log_Module SHALL 通过 @Log 注解 + AOP 切面自动采集操作日志，请求参数中的 password/oldPassword/newPassword 字段自动置空脱敏
8. THE Log_Module SHALL 在 AuthService 登录成功/失败时手动调用 LogService 记录登录日志，解析 User-Agent 获取浏览器、操作系统、设备类型信息
9. IF 日志记录失败, THEN THE Log_Module SHALL 仅记录 ERROR 日志，不影响业务操作正常返回
10. WHEN 后端实现日志管理接口, THE Log_Module SHALL 严格按照 `doc/design/modules/rbac/modules/日志管理/后端详细设计.md` 中定义的 10 个接口（LOG-001 至 LOG-010）实现
11. WHEN 前端实现日志管理页面, THE Log_Module SHALL 严格按照 `doc/design/modules/rbac/modules/日志管理/前端详细设计.md` 实现三个独立页面：操作日志页（/logs/system）、登录日志页（/logs/login）、在线用户页（/logs/online）

### 需求 8：默认租户与登录模式联调

**用户故事：** 作为系统用户，我希望登录页支持默认登录（单租户模式）和租户登录（多租户模式）两种方式，以便在当前单租户阶段简化登录流程，同时为未来多租户扩展做好准备。

#### 验收标准

1. WHEN 用户访问登录页, THE TenantLogin_Module SHALL 展示 Tabs 切换，包含"默认登录"和"租户登录"两个 Tab，默认选中"默认登录"
2. WHEN 用户在默认登录 Tab 提交（仅用户名+密码）, THE TenantLogin_Module SHALL 不传 tenantCode，后端直接使用代码常量 DEFAULT_TENANT_ID（1L），不查 sys_tenant 表，不校验租户状态
3. WHEN 用户在租户登录 Tab 选择具体租户并提交, THE TenantLogin_Module SHALL 传递 tenantCode，后端通过编码查询 sys_tenant 获取 tenant_id，完整校验租户存在性、状态和过期时间
4. WHEN 租户登录 Tab 的租户下拉框初始化, THE TenantLogin_Module SHALL 调用公开接口 TENANT-009 获取启用且未过期的租户列表（仅 code+name），在选项头部插入固定项"默认租户"（value 为空字符串）
5. IF 租户登录时租户编码不存在, THEN THE TenantLogin_Module SHALL 返回错误提示"租户不存在"
6. IF 租户登录时租户已禁用, THEN THE TenantLogin_Module SHALL 返回错误提示"租户已禁用"
7. IF 租户登录时租户已过期, THEN THE TenantLogin_Module SHALL 返回错误提示"租户已过期"
8. THE TenantLogin_Module SHALL 将 AuthServiceImpl.login() 方法重构为三个私有方法：resolveTenantId()（租户解析）、findAndValidateUser()（用户校验）、buildLoginResult()（Token 生成），使 login() 圈复杂度降为 1
9. THE TenantLogin_Module SHALL 将 LoginDTO 中的 tenantId 字段替换为可选的 tenantCode 字段（String 类型）
10. WHEN 后端实现默认租户与登录模式, THE TenantLogin_Module SHALL 严格按照 `doc/design/modules/rbac/modules/租户管理/默认租户与登录模式-详细设计.md` 实现
11. WHEN 前端改造登录页, THE TenantLogin_Module SHALL 严格按照 `doc/design/modules/rbac/modules/租户管理/默认租户与登录模式-前端详细设计.md` 实现 Tabs 切换和租户下拉框逻辑
