# 前端布局系统 - 实现任务清单

## 概述

本任务清单将前端布局系统的设计转化为具体的实现步骤。采用**原子化组件优先**的策略：先开发可复用的基础组件，再用组件组装页面。任务按照合理的执行顺序排列，从基础设施到集成测试，确保每个步骤都能增量验证核心功能。

## 开发原则

- **组件原子化**：每个组件职责单一，可独立复用
- **页面组装**：页面由原子组件组合而成
- **参考现有代码**：学习 userStore、request 拦截器等实现方式
- **Ant Design + React**：使用 Ant Design 组件库和 React 框架

## 任务列表

- [x] 1. 项目结构和基础设置
  - 创建必要的目录结构：
    - `frontend/src/components/layout/` - 布局组件
    - `frontend/src/components/common/` - 通用原子组件
    - `frontend/src/stores/` - Zustand stores
    - `frontend/src/services/` - API 服务
    - `frontend/src/types/` - TypeScript 类型定义
    - `frontend/src/styles/` - 全局样式
  - 创建 TypeScript 类型定义文件（types/layout.ts, types/menu.ts）
  - 参考 userStore 的模式创建 layoutStore 基础框架
  - _需求: 1.1, 14.1, 14.2, 14.3, 14.4, 14.5_

- [ ] 2. 创建 Zustand 状态管理 Store
  - [x] 2.1 实现 layoutStore（Zustand）
    - 参考 userStore 的模式实现
    - 定义 LayoutState 接口（sidebarCollapsed, systemMode, viewMode, theme, menuItems, menuLoading, menuError）
    - 实现状态初始化和本地存储持久化（参考 TOKEN_KEY 模式）
    - 实现 setSidebarCollapsed, setSystemMode, setViewMode, setTheme 等 action
    - 实现 initializeLayout 方法从本地存储恢复状态
    - _需求: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 2.2 扩展 userStore（如需要）
    - 如果需要在 userStore 中添加菜单权限相关的状态
    - 实现权限过滤逻辑
    - _需求: 4.2, 4.3, 4.4, 20.1, 20.2_

- [ ] 3. 创建 API 服务层
  - [x] 3.1 实现 menuApi 服务
    - 参考 userApi 的模式实现
    - 创建 getMenus() 方法获取菜单数据
    - 实现错误处理（参考 request 拦截器）
    - 实现菜单数据缓存机制
    - _需求: 7.1, 7.4, 15.4_

  - [x] 3.2 扩展 userApi 服务（如需要）
    - 如果需要添加获取用户菜单权限的接口
    - _需求: 4.2, 4.3_

- [ ] 4. 实现原子化组件 - Header 子组件
  - [x] 4.1 创建 Logo 组件（原子组件）
    - 显示系统名称和 Logo
    - Props: title?: string, logo?: React.ReactNode
    - 使用 Ant Design 样式
    - _需求: 1.1_

  - [x] 4.2 创建 SystemModeSelector 组件（原子组件）
    - 系统模式选择器下拉菜单（系统管理/应用系统）
    - Props: value, onChange, options
    - 使用 Ant Design Select 组件
    - _需求: 2.1, 2.2, 2.3, 2.6_

  - [x] 4.3 创建 ViewModeSelector 组件（原子组件）
    - 视图模式选择器（列表视图/卡片视图）
    - Props: value, onChange, options
    - 使用 Ant Design Segmented 或 Select 组件
    - _需求: 3.1, 3.2_

  - [x] 4.4 创建 ThemeToggle 组件（原子组件）
    - 主题切换按钮（亮色/暗色）
    - Props: value, onChange
    - 使用 Ant Design Button 或 Switch 组件
    - _需求: 18.1, 18.2, 18.3_

  - [x] 4.5 创建 UserMenu 组件（原子组件）
    - 用户下拉菜单（显示用户名、头像）
    - Props: userInfo, onLogout
    - 使用 Ant Design Dropdown 组件
    - _需求: 1.2, 1.3, 1.4, 4.1_

  - [x] 4.6 创建 SidebarToggle 组件（原子组件）
    - 侧边栏折叠/展开切换按钮
    - Props: collapsed, onChange
    - 使用 Ant Design Button 组件
    - _需求: 6.1_

- [ ] 5. 实现 Header 组件（组合组件）
  - [x] 5.1 创建 Header 组件
    - 整合 Logo、SystemModeSelector、ViewModeSelector、ThemeToggle、UserMenu、SidebarToggle 等原子组件
    - 实现 Header 组件框架（64px 高度，白色背景，底部边框）
    - 使用 Ant Design Layout.Header 组件
    - 连接 layoutStore 和 userStore
    - _需求: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 14.2_

  - [x] 5.2 实现 Header 中的状态管理逻辑
    - 系统模式切换：更新 layoutStore.systemMode，持久化到本地存储
    - 视图模式切换：更新 layoutStore.viewMode，持久化到本地存储
    - 主题切换：更新 layoutStore.theme，应用 Ant Design 主题定制，持久化到本地存储
    - 登出：调用 userStore.logout()，清除会话，重定向到登录页
    - _需求: 2.2, 2.4, 2.5, 3.3, 3.5, 3.6, 18.2, 18.4, 18.5, 4.2, 4.3, 4.4_

- [ ] 6. 实现原子化组件 - Sidebar 子组件
  - [x] 6.1 创建 MenuItem 组件（原子组件）
    - 单个菜单项，支持图标和标签
    - Props: icon, label, path, collapsed, onClick
    - 使用 Ant Design Menu.Item 组件
    - _需求: 5.2, 5.4_

  - [x] 6.2 创建 SubMenu 组件（原子组件）
    - 嵌套菜单项，支持展开/收起
    - Props: icon, label, children, collapsed
    - 使用 Ant Design Menu.SubMenu 组件
    - _需求: 5.5, 5.6_

  - [x] 6.3 创建 MenuList 组件（原子组件）
    - 菜单列表，支持权限过滤和加载状态
    - Props: items, collapsed, loading, error, onRetry, onItemClick
    - 使用 Ant Design Menu 组件
    - 实现权限过滤逻辑
    - 实现加载状态（Skeleton）和错误处理
    - _需求: 5.1, 5.3, 5.4, 7.2, 7.3, 19.1, 19.2, 19.3_

- [ ] 7. 实现 Sidebar 组件（组合组件）
  - [x] 7.1 创建 Sidebar 组件
    - 整合 MenuList 等原子组件
    - 实现 Sidebar 组件框架（使用 Ant Design Sider）
    - 实现菜单项点击导航（使用 React Router）
    - 实现当前活跃菜单项高亮
    - 连接 layoutStore 和 userStore
    - _需求: 5.1, 5.2, 5.3, 5.4, 5.7, 14.3_

  - [x] 7.2 实现 Sidebar 展开/收起功能
    - 实现 Sidebar 折叠状态管理（layoutStore.sidebarCollapsed）
    - 实现折叠时仅显示图标，隐藏标签
    - 实现本地存储持久化
    - 实现鼠标悬停时显示 Tooltip
    - _需求: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [x] 7.3 实现菜单数据加载
    - 集成 menuApi 获取菜单数据
    - 实现加载状态处理（显示 Skeleton）
    - 实现错误处理和重试机制
    - _需求: 7.1, 7.4, 15.4, 19.1, 19.2, 19.3_

- [ ] 8. 实现 Content 组件（原子组件）
  - [x] 8.1 创建 Content 组件
    - 实现 Content 组件框架（使用 Ant Design Layout.Content）
    - 实现子组件渲染
    - 实现响应式 padding 和 margin
    - 实现滚动支持
    - _需求: 8.1, 8.2, 8.3, 8.4, 14.4_

  - [x] 8.2 实现加载状态处理
    - 实现加载指示器（Skeleton）
    - 实现加载状态显示
    - _需求: 8.5, 15.5_

  - [x] 8.3 实现错误状态处理
    - 实现错误消息显示
    - 实现错误恢复机制
    - _需求: 8.6_

- [ ] 9. 实现 Layout 主容器组件（组合组件）
  - [x] 9.1 创建 Layout 主容器
    - 整合 Header、Sidebar、Content 三个主要组件
    - 实现布局结构（使用 Ant Design Layout）
    - 实现 showLayout 属性控制布局显示（登录页不显示）
    - 连接 layoutStore 和 userStore
    - _需求: 12.1, 12.2, 12.3, 12.4, 14.1_

  - [x] 9.2 实现布局初始化
    - 实现 initializeLayout 方法
    - 从本地存储恢复布局状态
    - 获取用户信息和菜单数据
    - _需求: 13.5_

  - [x] 9.3 实现响应式设计
    - 实现移动设备适配（<768px）：Sidebar 自动折叠
    - 实现平板设备适配（768px-1024px）：Sidebar 可见但可折叠
    - 实现桌面设备适配（>1024px）：Sidebar 默认展开
    - 使用 CSS 媒体查询和 Ant Design 响应式特性
    - _需求: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 10.1, 10.2, 10.3, 10.4, 11.1, 11.2, 11.3, 11.4_

- [ ] 10. 实现可访问性和权限功能
  - [x] 10.1 实现键盘导航
    - 实现 Tab 键导航菜单项
    - 实现 Enter 键激活菜单项
    - 实现 Escape 键关闭下拉菜单
    - 实现焦点指示器显示
    - _需求: 16.1, 16.2, 16.3, 16.4_

  - [x] 10.2 实现屏幕阅读器支持
    - 使用语义 HTML 元素（nav, header, main, aside）
    - 添加 ARIA 标签和角色
    - 为图标添加 alt 文本
    - 实现动态内容变化通知
    - _需求: 17.1, 17.2, 17.3, 17.4, 17.5_

  - [x] 10.3 实现权限检查和路由保护
    - 实现 hasPermission 方法
    - 实现路由权限检查
    - 实现权限不足时的重定向
    - 实现动态权限更新
    - _需求: 20.1, 20.2, 20.3, 20.4_

- [ ] 11. 实现错误处理和性能优化
  - [x] 11.1 实现菜单加载错误处理
    - 显示错误消息
    - 提供重试按钮
    - 实现重试逻辑
    - 记录错误日志
    - _需求: 19.1, 19.2, 19.3, 19.4_

  - [x] 11.2 实现性能优化
    - 使用 React.memo 优化菜单项
    - 使用 useMemo 优化计算
    - 使用 useCallback 优化事件处理
    - 实现菜单数据缓存
    - 实现 Skeleton 加载器
    - _需求: 15.1, 15.2, 15.3, 15.4, 15.5_

- [ ] 12. 集成和连接
  - [x] 12.1 集成 React Router
    - 实现路由检测（useLocation）
    - 实现活跃菜单项高亮
    - 实现路由保护
    - _需求: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [x] 12.2 集成现有页面
    - 在 App.tsx 中集成 Layout 组件
    - 在 LoginPage 中隐藏 Layout
    - 在 UserPage 中显示 Layout
    - 实现页面导航时的状态保留
    - _需求: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [x] 12.3 集成后端 API
    - 连接菜单数据 API
    - 连接用户信息 API
    - 实现认证集成
    - _需求: 7.1, 4.2, 4.3_

- [ ] 13. 最终检查和文档
  - [x] 13.1 验证所有功能
    - 验证所有功能正常工作
    - 验证响应式设计
    - 验证可访问性
    - 验证性能指标

  - [x] 13.2 编写文档
    - 为每个组件编写 JSDoc 注释
    - 编写使用示例
    - 编写集成指南

## 任务执行说明

### 开发策略

**原子化组件优先**：
1. 先开发小的、可复用的原子组件（Logo、SystemModeSelector、MenuItem 等）
2. 再用原子组件组合成组合组件（Header、Sidebar、Layout）
3. 最后集成到页面中

**参考现有代码**：
- 参考 `userStore` 的 Zustand 模式实现 `layoutStore`
- 参考 `request` 拦截器的错误处理方式
- 参考 `LoginPage` 的组件结构和样式

**技术选型**：
- React + TypeScript
- Ant Design v5 组件库
- Zustand 状态管理
- React Router v6 路由
- Axios 请求库

### 执行顺序

任务应按照上述顺序执行，每个任务都建立在前面任务的基础上：

1. **第 1 步**：项目结构和基础设置 - 为后续开发奠定基础
2. **第 2-3 步**：状态管理和 API 服务 - 实现数据层
3. **第 4-8 步**：原子组件和组合组件开发 - 实现 UI 层
4. **第 9 步**：Layout 主容器 - 整合所有组件
5. **第 10-11 步**：功能完善 - 可访问性、权限检查、错误处理、性能优化
6. **第 12 步**：集成 - 与现有系统集成
7. **第 13 步**：最终检查和文档 - 验证所有功能

### 组件设计原则

- **单一职责**：每个组件只做一件事
- **可复用性**：组件应该能在不同场景下复用
- **Props 驱动**：通过 Props 控制组件行为
- **状态管理**：使用 Zustand 管理全局状态，组件内部状态最小化
- **样式隔离**：使用 Ant Design 样式，避免全局样式污染

