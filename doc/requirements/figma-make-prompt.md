> **⚠️ 部分内容已过期（2026-09-01）**：本仓库已拿掉多租户机制，本文中的「Tenant
> Management（租户管理）」页面 Prompt 已不适用，该页面已删除。其余页面 Prompt 仍可参考。
> 原因见 `doc/design/modules/core/去多租户化-概要设计.md`。

# Figma Make Prompts - 车联网平台

> **文档类型**: Figma Make 提示词文档
> **版本**: v1.0.0
> **创建日期**: 2026-03-30
> **说明**: 将每个页面的 Prompt 复制到 Figma Make 中生成静态页面

---

## Part 1: Global Design System

> **使用方法**: 将以下设计系统定义添加到每个页面 Prompt 的开头，确保所有页面风格统一。

```
Design System:
- Style: Enterprise SaaS management platform, clean and professional
- Color Palette:
  - Primary: #1677ff (brand blue)
  - Success: #52c41a (green)
  - Warning: #faad14 (orange)
  - Error/Danger: #ff4d4f (red)
  - Neutral: #1f1f1f (text), #595959 (secondary text), #8c8c8c (disabled), #bfbfbf (border), #f0f0f0 (divider), #fafafa (table header bg), #f5f5f5 (page background)
  - Background: #ffffff (card/container), #f5f5f5 (page bg), #001529 (sidebar dark)
- Typography:
  - Font Family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", sans-serif
  - Page Title: 20px, font-weight 600, color #1f1f1f
  - Section Title: 16px, font-weight 600
  - Body: 14px, font-weight 400, color #1f1f1f
  - Secondary Text: 14px, color #595959
  - Caption: 12px, color #8c8c8c
- Spacing: 8px grid system, common values: 8, 12, 16, 24, 32, 48
- Border Radius: 6px (buttons, cards, inputs), 4px (tags)
- Shadows:
  - Card: 0 1px 2px rgba(0,0,0,0.03), 0 1px 6px -1px rgba(0,0,0,0.02), 0 2px 4px rgba(0,0,0,0.02)
  - Modal: 0 6px 16px rgba(0,0,0,0.08), 0 3px 6px -4px rgba(0,0,0,0.12), 0 9px 28px 8px rgba(0,0,0,0.05)
- Components:
  - Buttons: height 32px, padding 0 15px, border-radius 6px. Primary: bg #1677ff, text white. Default: bg white, border #d9d9d9. Danger: bg #ff4d4f, text white. Link: no bg, text #1677ff
  - Tags: height 22px, padding 0 7px, border-radius 4px. Success=green, Error=red, Warning=orange, Default=gray, Primary=blue
  - Table: header bg #fafafa, row height 54px, border-bottom 1px solid #f0f0f0, hover bg #fafafa
  - Input: height 32px, border 1px solid #d9d9d9, border-radius 6px, focus border #1677ff
  - Select: same as input, with dropdown arrow icon
  - Modal: width 600px, header with title + close icon, footer with Cancel + OK buttons
  - Drawer: slides from right, width 600px
  - Sidebar: width 220px (collapsed 80px), dark bg #001529, text white, active item bg #1677ff
  - Pagination: total count, page size selector, page numbers
- Icons: Use Ant Design Icons style (outlined, 14px or 16px)
- Language: UI text in Chinese (简体中文)
```

---

## Part 2: Layout Templates

### 2.1 Auth Layout (Login Page)

```
Design System: [Copy Part 1 Design System above]

Create a login page for a vehicle IoT SaaS platform (车联网平台).

Layout:
- Full-screen background with a subtle gradient from #e6f0ff (top-left) to #f5f5f5 (bottom-right)
- On the left half: a decorative illustration area showing a connected car / fleet management concept with abstract geometric shapes in brand blue (#1677ff) tones. Include the platform logo at top-left with text "Precision 车联网平台"
- On the right half, centered vertically and horizontally: a white card (width 400px, rounded 12px, with subtle shadow)
  - Card header: Logo icon + "欢迎登录" (Welcome) as title (24px, bold) + "车联网管理平台" subtitle (14px, gray)
  - Form fields with vertical layout (gap 24px):
    - Username input with user icon prefix, placeholder "请输入用户名"
    - Password input with lock icon prefix, placeholder "请输入密码", with eye toggle icon
    - Row: "记住密码" checkbox (left) + "忘记密码?" link text (right, color #1677ff)
    - Login button: full width, height 40px, bg #1677ff, text "登 录" (white, 16px, bold), border-radius 6px
  - Bottom text: "还没有账号？联系管理员" (12px, gray)

The page should feel modern, trustworthy, and enterprise-grade.
```

---

### 2.2 RBAC Admin Layout

```
Design System: [Copy Part 1 Design System above]

Create the main admin layout for an enterprise management platform. This is the shell/frame that wraps all RBAC admin pages.

Layout:
- Full viewport height, no scrolling on the outer frame
- Left Sidebar (width 220px, bg #001529, text white):
  - Top: Logo area (height 64px) with icon + "Precision" text, centered, border-bottom 1px solid rgba(255,255,255,0.1)
  - Navigation Menu (vertical, with icons):
    - Section "系统管理" (with folder icon):
      - 用户管理 (user icon)
      - 角色管理 (team icon)
      - 菜单管理 (menu icon)
      - 部门管理 (cluster icon)
      - 字典管理 (book icon)
    - Section "系统监控" (with monitor icon):
      - 在线用户 (online icon)
      - 操作日志 (file icon)
      - 登录日志 (login icon)
  - Bottom: Collapse button (double-left arrow icon)
  - Active menu item: bg #1677ff with white text
  - Hover: bg rgba(255,255,255,0.08)
- Right area:
  - Top Header (height 64px, bg white, border-bottom 1px solid #f0f0f0):
    - Left side: Breadcrumb "首页 / 系统管理 / 用户管理"
    - Right side: notification bell icon (with red dot badge) + user avatar (32px circle) + "管理员" dropdown
  - Content Area (bg #f5f5f5, padding 24px, scrollable):
    - Show placeholder text "Content Area" to indicate where page content renders

Use Chinese text. The layout should feel clean and organized like Ant Design Pro.
```

---

### 2.3 Business Layout (Dashboard)

```
Design System: [Copy Part 1 Design System above]

Create the business platform layout with a dashboard home page. This layout is for daily business operations (vehicle monitoring, device management, etc.), separate from the RBAC admin layout.

Layout Shell:
- Same structure as RBAC Admin Layout but different sidebar menu:
  - Left Sidebar (width 220px, bg #001529):
    - Logo: "Precision" with car icon
    - Navigation Menu:
      - 首页 (home icon) ← active
      - 实时监控 (monitor icon)
      - 车辆管理 (car icon)
        - 车辆列表
        - 轨迹回放
      - 设备管理 (device icon)
      - 报警管理 (alert icon)
      - 统计报表 (chart icon)
      - 系统管理 (setting icon)

Dashboard Content (inside the content area):
- Row 1: 4 stat cards in a row (gap 16px)
  - Card 1: "在线设备" (Online Devices) — icon: signal, value: 1,234, color: blue, subtitle "较昨日 +12%"
  - Card 2: "活跃车辆" (Active Vehicles) — icon: car, value: 856, color: green, subtitle "较昨日 +5%"
  - Card 3: "今日报警" (Today Alerts) — icon: alert, value: 23, color: orange, subtitle "较昨日 -3%"
  - Card 4: "离线设备" (Offline Devices) — icon: poweroff, value: 45, color: red, subtitle "需要关注"
  - Each card: white bg, rounded 6px, padding 20px, left side icon circle (48px) + right side value and label

- Row 2: 2 charts side by side (gap 16px, height 320px)
  - Left chart (span 16): "设备在线趋势" (Device Online Trend) — area chart showing 7-day data, brand blue fill with gradient
  - Right chart (span 8): "报警类型分布" (Alert Distribution) — donut chart with 4 segments: overspeed (blue), fatigue (orange), geofence (green), offline (red)

- Row 3: 2 panels side by side (gap 16px)
  - Left panel: "最近报警" (Recent Alerts) — a compact table with 5 rows: time, device SN, vehicle plate, alert type (tag), status (tag)
  - Right panel: "设备状态分布" (Device Status) — horizontal bar chart showing online/offline/fault distribution

Use Chinese text. Dashboard should feel data-rich but not cluttered.
```

---

## Part 3: RBAC Page Prompts

### 3.1 Tenant Management

```
Design System: [Copy Part 1 Design System above]

Create a Tenant Management (租户管理) page for a multi-tenant SaaS platform.

Page Title: "租户管理" with breadcrumb "首页 / 租户管理"

Layout - Standard list page:

1. Search Bar (white card, padding 16px 24px):
   - Row 1: Input "租户名称" (width 200px) + Input "租户编码" (width 200px) + Select "状态" with options "全部/启用/禁用" (width 120px) + Button "查询" (primary) + Button "重置" (default)

2. Action Bar (below search, margin-top 16px):
   - Left: Button "新增租户" (primary, with PlusOutlined icon)
   - Right: Button "刷新" (default, with ReloadOutlined icon) + Button "导出" (default, with DownloadOutlined icon)

3. Table (white card):
   Columns (left to right):
   - 租户编码: 120px, text "TENANT001"
   - 租户名称: 180px, text "顺丰速运"
   - 联系人: 100px, text "张三"
   - 联系电话: 120px, text "13800138000"
   - 设备数量: 100px, number "150"
   - 用户数量: 100px, number "25"
   - 到期时间: 150px, date "2027-12-31"
   - 状态: 80px, Tag green "正常" or Tag red "已过期"
   - 操作: 200px, buttons: "详情" (link) + "编辑" (link) + "配置" (link) + "禁用" (link, red)

   Show 5 sample rows with alternating visual weight.

4. Pagination (below table, right-aligned):
   - "共 56 条" + page size selector "10 条/页" + page buttons "< 1 2 3 ... 6 >"

5. Create Tenant Modal (overlay, width 640px):
   - Title: "新增租户"
   - Form (vertical layout, 2-column grid for short fields):
     - 租户编码 * : Input, placeholder "请输入租户编码"
     - 租户名称 * : Input, placeholder "请输入租户名称"
     - 联系人 * : Input
     - 联系电话 * : Input
     - 联系邮箱: Input
     - 联系地址: Input (full width)
     - 到期时间: DatePicker (full width)
     - 账号限额: InputNumber, default 100
     - 设备限额: InputNumber, default 1000
     - 状态: Switch, default ON
     - 备注: TextArea (2 rows, full width)
   - Footer: Button "取消" (default) + Button "确定" (primary)

Use Chinese text. Professional enterprise management style.
```

---

### 3.2 User Management

```
Design System: [Copy Part 1 Design System above]

Create a User Management (用户管理) page with a left department tree and right user list.

Page Title: "用户管理" with breadcrumb "首页 / 系统管理 / 用户管理"

Layout - Split view (tree + list):

1. Left Panel (width 280px, white card, full height):
   - Header: "组织架构" (14px, bold) with a search input below (placeholder "搜索部门...")
   - Tree structure:
     - ▼ 总公司 (25) ← selected, highlight bg #e6f0ff
       - 运营部 (8)
       - 技术部 (10)
       - 财务部 (7)
     - ▼ 分公司 (15)
       - 华东分公司 (5)
       - 华南分公司 (10)
   - Each node: folder icon + name + user count in parentheses (gray)
   - Clicking a department filters the right-side table

2. Right Panel (flex: 1, margin-left 16px):
   a. Search Bar:
      - Row: Input "用户名" (160px) + Input "手机号" (160px) + Select "状态" (100px, 全部/启用/禁用) + Button "搜索" (primary) + Button "重置"

   b. Action Bar:
      - Left: Button "新增用户" (primary, PlusOutlined)
      - Right: Button "刷新" (default)

   c. Table:
      Columns:
      - 用户名: 120px, "admin"
      - 昵称: 100px, "管理员"
      - 部门: 120px, "运营部"
      - 手机号: 120px, "138****8000"
      - 邮箱: 160px, "admin@example.com"
      - 职务: 100px, "部门经理"
      - 状态: 80px, Tag green "启用" or Tag red "禁用"
      - 创建时间: 150px, "2026-01-15 10:30"
      - 操作: 240px, buttons: "详情" + "编辑" + "删除" (red) + "重置密码" + "分配角色"
      Show 5 rows.

   d. Pagination: "共 25 条" + "< 1 2 3 >"

3. Create User Modal (width 680px):
   - Title: "新增用户"
   - Form in sections:
     Section "基本信息":
       - 用户名 * : Input (half width) + 昵称 * : Input (half width)
       - 所属部门 * : TreeSelect (half width) + 手机号 * : Input (half width)
       - 邮箱: Input (half width) + 性别: Radio "男/女/未知" (half width)
       - 职务: Select with options "CEO/总监/经理/主管/工程师/司机" (half width)
       - 状态: Switch (default ON)
     Section "密码设置":
       - 密码 * : InputPassword (half width)
       - 确认密码 * : InputPassword (half width)
       - Hint text: "8-20位，需包含大小写字母和数字" (12px, gray)
     Section "角色分配":
       - 角色: Select mode="multiple", showing tags like "管理员 ×" "运营人员 ×"
     - 备注: TextArea (full width, 2 rows)
   - Footer: "取消" + "确定"

Use Chinese text. The split layout should feel balanced with the tree taking about 25% width.
```

---

### 3.3 Role Management

```
Design System: [Copy Part 1 Design System above]

Create a Role Management (角色管理) page for RBAC permission control.

Page Title: "角色管理" with breadcrumb "首页 / 系统管理 / 角色管理"

Layout - Standard list page:

1. Search Bar:
   - Input "角色名称" (160px) + Input "角色编码" (160px) + Select "状态" (100px) + Button "查询" + Button "重置"

2. Action Bar:
   - Left: Button "新增角色" (primary)
   - Right: Button "刷新" + Button "导出"

3. Table:
   Columns:
   - 角色编码: 120px, "ADMIN"
   - 角色名称: 140px, "管理员"
   - 数据权限: 140px, Tag blue "全部数据" / Tag green "本部门数据" / Tag orange "仅本人数据"
   - 用户数量: 100px, "3"
   - 排序: 80px, "1"
   - 状态: 80px, Tag green "启用" or Tag red "禁用"
   - 创建时间: 150px
   - 操作: 320px, buttons: "编辑" + "权限" (blue) + "数据权限" (blue) + "用户" + "删除" (red, only when user count = 0)
   Show 5 rows with different roles: ADMIN, USER, OPERATOR, VIEWER, DRIVER

4. Pagination: standard

5. Assign Menu Permission Modal (width 720px):
   - Title: "分配菜单权限 - 管理员"
   - Top actions: Checkbox "全选/取消全选" + Button "展开" + Button "折叠"
   - Tree with checkboxes:
     ☑ 系统管理 (folder)
       ☑ 用户管理 (menu)
         ☑ 查询 (button, gray tag)
         ☑ 新增 (button, gray tag)
         ☑ 编辑 (button, gray tag)
         ☑ 删除 (button, gray tag)
       ☑ 角色管理 (menu)
       ☐ 菜单管理 (menu)
       ☐ 部门管理 (menu)
     ☐ 监控中心 (folder)
   - Each node shows: checkbox + icon + name + type tag (目录=blue, 菜单=green, 按钮=gray)
   - Footer: "取消" + "确定"

6. Data Scope Setting Modal (width 520px):
   - Title: "设置数据权限 - 运营人员"
   - Radio group:
     ○ 全部数据
     ○ 本部门及子部门数据
     ○ 本部门数据
     ● 仅本人数据
     ○ 自定义
   - When "自定义" selected, show a department tree with checkboxes below
   - Footer: "取消" + "确定"

7. Associated Users Modal (width 700px):
   - Title: "关联用户 - 管理员"
   - Search: Input "用户名" + Button "搜索"
   - Table: 用户名 / 昵称 / 部门 / 状态 / 操作 (Button "移除" red)
   - Show 3 rows
   - Bottom: "共 3 条"

Use Chinese text.
```

---

### 3.4 Menu Management

```
Design System: [Copy Part 1 Design System above]

Create a Menu Management (菜单管理) page with a tree table showing the 3-level menu hierarchy (Directory > Menu > Button).

Page Title: "菜单管理" with breadcrumb "首页 / 系统管理 / 菜单管理"

Layout - Tree table page:

1. Action Bar (no search bar):
   - Left: Button "新增菜单" (primary, PlusOutlined)
   - Right: Button "展开全部" (default) + Button "收起全部" (default) + Button "刷新"

2. Tree Table (white card, rows are indented to show hierarchy):
   Columns:
   - 菜单名称: 250px, tree structure with expand/collapse arrows and icons:
     ▼ 📁 系统管理 (indent 0, type=目录)
       👤 用户管理 (indent 1, type=菜单)
         新增 (indent 2, type=按钮)
         编辑 (indent 2, type=按钮)
       🔑 角色管理 (indent 1, type=菜单)
     ▼ 📊 监控中心 (indent 0, type=目录)
       在线用户 (indent 1)
   - 图标: 80px, show icon or "-" for buttons
   - 排序: 80px, numbers
   - 权限标识: 200px, "system:user" or "system:user:add", or "-" for directories
   - 类型: 80px, Tag blue "目录" / Tag green "菜单" / Tag orange "按钮"
   - 状态: 80px, Tag green "正常"
   - 操作: 150px, buttons: "新增" (for directory/menu) + "编辑" + "删除" (with Popconfirm)

   Show complete tree with: 系统管理 (用户管理 with 4 buttons, 角色管理 with 2 buttons, 菜单管理, 部门管理, 字典管理) + 监控中心 (在线用户, 操作日志, 登录日志)

3. Create Menu Modal (width 640px):
   - Title: "新增菜单"
   - Type selector at top: Radio buttons "目录" / "菜单" / "按钮" (with icons)

   - Common fields:
     - 上级菜单: TreeSelect, showing menu tree
     - 菜单名称 * : Input

   - Conditional fields (show based on type):
     When "目录" selected:
       - 图标: Icon picker
       - 路由地址: Input (placeholder "/system")

     When "菜单" selected:
       - 图标: Icon picker
       - 路由地址 * : Input (placeholder "/system/user")
       - 组件路径 * : Input (placeholder "system/user/index")
       - 权限标识: Input (placeholder "system:user")
       - 是否外链: Switch
       - 是否缓存: Switch
       - 是否显示: Switch (default ON)

     When "按钮" selected:
       - 权限标识 * : Input (placeholder "system:user:add")

   - Common bottom fields:
     - 排序: InputNumber (0-999)
     - 状态: Switch (default ON)
   - Footer: "取消" + "确定"

Use Chinese text. The tree table should clearly show the 3-level hierarchy.
```

---

### 3.5 Department Management

```
Design System: [Copy Part 1 Design System above]

Create a Department Management (部门管理) page with a tree table showing the organization hierarchy.

Page Title: "部门管理" with breadcrumb "首页 / 系统管理 / 部门管理"

Layout - Tree table page:

1. Search Bar:
   - Input "搜索部门..." (200px, with search icon) + Button "展开" (default) + Button "收起" (default) + Button "新增部门" (primary, right-aligned)

2. Tree Table (white card):
   Columns:
   - 部门名称: 280px, tree structure with expand arrows and user counts:
     ▼ 总公司 (25) ← top level
       运营部 (8) ← indent 1
       技术部 (10) ← indent 1
       财务部 (7) ← indent 1
     ▼ 分公司 (15) ← top level
       ▶ 华东分公司 (5) ← indent 1, collapsed
       ▶ 华南分公司 (10) ← indent 1, collapsed
   - 负责人: 120px, "张三"
   - 联系电话: 130px, "13800138000"
   - 用户数: 80px, number
   - 状态: 80px, Tag green "正常"
   - 排序: 80px, number
   - 操作: 180px, buttons: "新增" + "编辑" + "删除" (only when user count = 0, otherwise hidden/disabled)

3. Create Department Modal (width 560px):
   - Title: "新增部门"
   - Form (vertical):
     - 上级部门: TreeSelect (showing department tree)
     - 部门名称 * : Input
     - 负责人: Select with user search
     - 联系电话: Input
     - 邮箱: Input
     - 排序: InputNumber (0-999)
     - 状态: Switch (default ON)
   - Footer: "取消" + "确定"

Use Chinese text. Tree hierarchy should be visually clear with proper indentation.
```

---

### 3.6 Dictionary Management

```
Design System: [Copy Part 1 Design System above]

Create a Dictionary Management (字典管理) page with a dictionary type list and a drill-in data management panel.

Page Title: "字典管理" with breadcrumb "首页 / 系统管理 / 字典管理"

Layout - Standard list page:

1. Search Bar:
   - Input "字典名称" (160px) + Input "字典类型" (160px) + Select "状态" (100px) + Button "查询" + Button "重置"

2. Action Bar:
   - Left: Button "新增类型" (primary)
   - Right: Button "刷新" + Button "刷新缓存" (default, with icon)

3. Table:
   Columns:
   - 字典名称: 160px, "用户性别"
   - 字典类型: 160px, monospace text "sys_user_gender" (copyable)
   - 数据项数: 100px, "3"
   - 状态: 80px, Tag green "正常"
   - 备注: 200px
   - 创建时间: 150px
   - 操作: 200px, buttons: "数据" (primary link) + "编辑" + "删除" (only when data count = 0)
   Show 6 rows: 用户性别, 用户职务, 系统开关, 显示状态, 菜单类型, 数据权限范围

4. Dictionary Data Panel (Navigated page, replaces the list):
   - Breadcrumb: "首页 / 系统管理 / 字典管理 / 用户性别"
   - Header info: "字典类型: sys_user_gender" + "字典名称: 用户性别"
   - Action Bar: Button "新增数据项" (primary) + Button "刷新缓存"
   - Data Table:
     Columns:
     - 字典标签: 150px, "男"
     - 字典键值: 100px, "1"
     - 排序: 80px, "1"
     - 样式属性: 100px, show Tag with actual color: Tag blue "primary", Tag red "danger", Tag gray "default"
     - 是否默认: 80px, Tag green "是" or Tag gray "否"
     - 状态: 80px, Tag green "正常"
     - 操作: 150px, "编辑" + "删除"
     Show 3 rows for gender dict: 男(1, primary), 女(2, danger), 未知(0, default, is_default=是)

5. Create Dictionary Type Modal (width 560px):
   - Title: "新增字典类型"
   - Form: 字典名称 * + 字典类型 * + 状态 (Switch) + 备注 (TextArea)
   - Footer: "取消" + "确定"

Use Chinese text. Dictionary data should show actual colored tags matching their css_class value.
```

---

### 3.7 Operation Log

```
Design System: [Copy Part 1 Design System above]

Create an Operation Log (操作日志) page for audit tracking.

Page Title: "操作日志" with breadcrumb "首页 / 系统监控 / 操作日志"

Layout - Standard list page:

1. Search Bar:
   - Input "操作模块" (140px) + Select "操作类型" (120px, options: 全部/新增/修改/删除/导出) + Input "操作人员" (120px) + Select "操作状态" (100px, 全部/成功/失败) + DatePicker "操作时间" (date range, 260px) + Button "查询" + Button "重置"

2. Action Bar:
   - Left: (no create button, logs are auto-generated)
   - Right: Button "清理日志" (default, with DeleteOutlined) + Button "导出" + Button "刷新"

3. Table:
   Columns:
   - 操作模块: 100px, "用户管理"
   - 操作类型: 80px, colored Tag: 新增=green, 修改=blue, 删除=red, 导出=orange
   - 操作描述: 200px, "新增用户[张三]"
   - 操作人员: 90px, "admin"
   - 操作IP: 120px, "192.168.1.100"
   - 操作地点: 100px, "北京市"
   - 操作状态: 80px, Tag green "成功" or Tag red "失败"
   - 耗时: 80px, "156ms"
   - 操作时间: 150px
   - 操作: 80px, Button "详情" (link)
   Show 8 rows with mixed operation types.

4. Pagination: standard

5. Detail Drawer (slides from right, width 640px):
   - Title: "操作日志详情"
   - Section "基本信息" (Descriptions layout, 2 columns):
     - 操作模块: 用户管理
     - 操作类型: Tag green "新增"
     - 操作描述: 新增用户[张三]
     - 操作人员: admin
     - 操作IP: 192.168.1.100
     - 操作地点: 北京市朝阳区
     - 操作状态: ✓ 成功
     - 执行耗时: 156ms
     - 操作时间: 2026-03-15 10:30:00
   - Section "请求信息" (code block style, bg #f5f5f5, monospace):
     - 请求方法: POST
     - 请求URL: /api/v1/users
     - 请求参数: JSON code block
   - Section "响应信息" (code block):
     - 响应结果: JSON code block

Use Chinese text. The log detail drawer should show structured technical information.
```

---

### 3.8 Login Log

```
Design System: [Copy Part 1 Design System above]

Create a Login Log (登录日志) page for login behavior analysis.

Page Title: "登录日志" with breadcrumb "首页 / 系统监控 / 登录日志"

Layout - Standard list page:

1. Search Bar:
   - Input "用户名" (140px) + Select "登录状态" (120px, 全部/成功/失败) + DatePicker "登录时间" (date range) + Button "查询" + Button "重置"

2. Action Bar:
   - Right: Button "清理日志" + Button "导出" + Button "刷新"

3. Table:
   Columns:
   - 用户名: 100px, "admin"
   - 登录方式: 100px, Tag blue "密码" or Tag green "验证码"
   - 登录IP: 120px, "192.168.1.100"
   - 登录地点: 120px, "北京市朝阳区"
   - 浏览器: 100px, "Chrome 120"
   - 操作系统: 100px, "Windows 11"
   - 登录状态: 80px, Tag green "成功" or Tag red "失败"
   - 提示消息: 150px, "登录成功" or "密码错误"
   - 登录时间: 150px
   - 操作: 80px, Button "详情"
   Show 6 rows (4 success, 2 failed).

4. Detail Drawer (width 560px):
   - Title: "登录日志详情"
   - Section "登录信息":
     - 用户名: admin
     - 登录方式: 密码
     - 登录IP: 192.168.1.100
     - 登录地点: 北京市朝阳区
     - 浏览器: Chrome 120
     - 操作系统: Windows 11
     - 登录状态: ✓ 成功
     - 提示消息: 登录成功
     - 登录时间: 2026-03-15 10:30:00
   - Section "设备信息":
     - User-Agent: (monospace text)
     - 设备类型: 电脑

Use Chinese text.
```

---

### 3.9 Online Users

```
Design System: [Copy Part 1 Design System above]

Create an Online Users (在线用户) page for session management.

Page Title: "在线用户" with breadcrumb "首页 / 系统监控 / 在线用户"

Layout - Compact list page:

1. Search Bar:
   - Input "用户名" (160px) + Button "搜索"
   - Right side: Badge "当前在线: 12 人" (blue)

2. Table:
   Columns:
   - 用户名: 100px, "admin"
   - 昵称: 100px, "管理员"
   - 部门: 120px, "运营部"
   - 登录IP: 120px, "192.168.1.100"
   - 登录地点: 100px, "北京市"
   - 浏览器: 100px, "Chrome 120"
   - 操作系统: 100px, "Windows 11"
   - 登录时间: 150px, "2026-03-15 10:30:00"
   - 操作: 80px, Button "强退" (red link, with Popconfirm "确定强制下线该用户？")
   Show 5 rows. No pagination needed (small dataset).

Use Chinese text. Compact and clean layout.
```

---

## Part 4: Reusable Component Prompts

### 4.1 Form Modal (Generic)

```
Design System: [Copy Part 1 Design System above]

Create a generic form modal component for enterprise management systems.

- Width: 640px
- Centered on screen with dimmed overlay (rgba(0,0,0,0.45))
- Header: Title text (16px, bold) on the left, X close button on the right, border-bottom 1px solid #f0f0f0, padding 16px 24px
- Body: padding 24px, form with vertical layout
  - Form items: label (14px, color #1f1f1f) on top, input below, gap 24px
  - Required fields marked with red asterisk * before label
  - 2-column grid for short fields, full width for long fields
- Footer: right-aligned, padding 10px 24px, border-top 1px solid #f0f0f0
  - Button "取消" (default) + Button "确定" (primary, bg #1677ff)

Show example with typical enterprise form fields.
Use Chinese text.
```

---

### 4.2 Permission Tree

```
Design System: [Copy Part 1 Design System above]

Create a permission tree component for assigning menu and button permissions to roles.

- Container: white bg, border 1px solid #f0f0f0, border-radius 6px, padding 16px
- Header: Checkbox "全选/取消全选" + Button "展开" + Button "折叠"
- Tree structure with checkboxes:
  - ☑ System Management (folder icon, blue tag "目录")
    - ☑ User Management (user icon, green tag "菜单")
      - ☑ Query (no icon, orange tag "按钮")
      - ☑ Add (no icon, orange tag "按钮")
      - ☐ Edit (no icon, orange tag "按钮")
    - ☐ Role Management (key icon, green tag "菜单")
  - ☐ Monitoring Center (monitor icon, blue tag "目录")
- Indent: 24px per level
- Checked items: blue checkbox with blue text
- Unchecked items: empty checkbox with default text
- Partially checked parent: blue filled checkbox (indeterminate state)

Use Chinese labels for the tree items.
```

---

### 4.3 Transfer (Role Assignment)

```
Design System: [Copy Part 1 Design System above]

Create a Transfer (穿梭框) component for assigning roles to users.

- Two-panel layout, side by side
- Left panel "可选角色" (Available Roles):
  - Header with count "共 5 项", bg #fafafa, border-bottom
  - Search input at top
  - List of items with checkbox:
    ☐ 普通用户 (USER)
    ☐ 观察员 (VIEWER)
    ☐ 司机 (DRIVER)
- Middle: Arrow buttons "▶" (move right) and "◀" (move left)
- Right panel "已选角色" (Selected Roles):
  - Header with count "已选 2 项", bg #fafafa, border-bottom
  - List of items:
    ✓ 管理员 (ADMIN)
    ✓ 运营人员 (OPERATOR)
- Width: each panel 280px, total width ~620px
- Each item: checkbox + role name + role code in gray

Use Chinese text.
```

---

## 使用指南

### 快速开始

1. 复制 **Part 1: Global Design System** 的内容
2. 将其粘贴到页面 Prompt 的 `[Copy Part 1 Design System above]` 占位符处
3. 复制完整 Prompt 到 Figma Make
4. 等待生成，检查结果

### 推荐生成顺序

| 顺序 | 页面 | 原因 |
|------|------|------|
| 1 | Login Page | 最简单，验证设计系统 |
| 2 | RBAC Admin Layout | 验证整体框架 |
| 3 | Business Layout + Dashboard | 验证第二套框架 |
| 4 | User Management | 最复杂的页面，包含树+列表+弹窗 |
| 5 | Role Management | 包含权限树等复杂交互 |
| 6 | Menu Management | 树形表格 |
| 7 | Department Management | 树形表格（简单版） |
| 8 | Tenant Management | 标准列表页模板 |
| 9 | Dictionary Management | 两级页面 |
| 10 | Operation Log | 日志列表+抽屉 |
| 11 | Login Log | 简单列表 |
| 12 | Online Users | 最简单的列表 |

### 注意事项

- 每次生成一个页面，不要合并多个页面
- 生成后检查是否与 PRD 的 ASCII 布局图一致
- Figma Make 可能需要微调，生成后手动调整细节
- 弹窗组件建议单独生成后再组装
