# 前端布局系统设计文档

## Overview

前端布局系统是应用的核心框架，为所有业务页面提供统一的视觉和交互体验。该系统采用经典的三层布局结构：顶部 Header、左侧 Sidebar 和主内容区域 Content，支持系统模式切换、菜单导航、用户信息展示、响应式设计等功能。

### 核心目标

1. 提供统一的应用框架，确保所有页面具有一致的外观和交互体验
2. 支持灵活的菜单导航和权限管理
3. 实现响应式设计，适配不同屏幕尺寸
4. 提供高效的状态管理和性能优化
5. 确保可访问性和用户体验

### 关键特性

- Header：系统名称、用户信息、系统切换、视图切换、主题切换
- Sidebar：菜单导航、展开/收起、权限过滤、嵌套菜单支持
- Content：主内容区域、响应式布局、加载状态处理
- 状态管理：使用 Zustand 管理布局状态和用户偏好
- 响应式设计：支持移动设备、平板和桌面设备

---

## Architecture

### 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                      Header (64px)                       │
│  Logo | System Mode | View Mode | Theme | User Menu     │
└─────────────────────────────────────────────────────────┘
┌──────────────┬──────────────────────────────────────────┐
│              │                                          │
│   Sidebar    │          Content Area                    │
│  (200px)     │      (Responsive Width)                  │
│              │                                          │
│  - Menu      │  - Page Content                          │
│  - Icons     │  - Loading State                         │
│  - Collapse  │  - Error Handling                        │
│              │                                          │
└──────────────┴──────────────────────────────────────────┘
```

### 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│  ┌──────────────┬──────────────┬──────────────────────┐ │
│  │   Header     │   Sidebar    │   Content            │ │
│  │  Component   │  Component   │   Component          │ │
│  └──────────────┴──────────────┴──────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   State Management Layer                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Zustand Store (layoutStore, userStore)         │   │
│  │  - Layout State (sidebar, theme, viewMode)      │   │
│  │  - User State (userInfo, permissions)           │   │
│  │  - Menu State (menuItems, loading)              │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Service Layer                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  API Services (menuApi, userApi, etc.)          │   │
│  │  - Fetch menu data                              │   │
│  │  - Fetch user info                              │   │
│  │  - Handle authentication                        │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 数据流向

```
User Interaction
       ↓
Component Event Handler
       ↓
Zustand Store Action
       ↓
State Update
       ↓
Component Re-render
       ↓
UI Update
```

---

## Components and Interfaces

### 1. Layout 组件（主容器）

**职责**：整合 Header、Sidebar、Content 三个主要组件，管理整体布局

**Props**：
```typescript
interface LayoutProps {
  children: React.ReactNode;
  showLayout?: boolean; // 是否显示布局（登录页不显示）
}
```

**功能**：
- 根据 showLayout 决定是否显示 Header 和 Sidebar
- 响应式处理：根据屏幕宽度调整布局
- 集成 Zustand store 获取布局状态

---

### 2. Header 组件

**职责**：显示系统名称、用户信息、系统切换、视图切换、主题切换

**Props**：
```typescript
interface HeaderProps {
  onCollapseSidebar?: () => void;
  collapsed?: boolean;
}
```

**子组件**：
- **Logo 区域**：系统名称和 Logo
- **System Mode Selector**：系统管理 / 应用系统 切换
- **View Mode Selector**：列表视图 / 卡片视图 切换
- **Theme Toggle**：亮色 / 暗色 主题切换
- **User Menu**：用户信息、退出登录

**功能**：
- 显示当前登录用户名和头像
- 系统模式切换并持久化
- 视图模式切换并持久化
- 主题切换并持久化
- 用户下拉菜单（退出登录）

---

### 3. Sidebar 组件

**职责**：显示菜单导航，支持展开/收起、权限过滤、嵌套菜单

**Props**：
```typescript
interface SidebarProps {
  collapsed?: boolean;
  onCollapse?: (collapsed: boolean) => void;
}
```

**功能**：
- 根据用户权限动态加载菜单项
- 支持菜单展开/收起
- 高亮当前活跃菜单项
- 支持嵌套菜单（子菜单）
- 收起时显示 Tooltip
- 菜单项点击导航

**菜单数据结构**：
```typescript
interface MenuItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  path?: string;
  children?: MenuItem[];
  permissions?: string[];
  visible?: boolean;
}
```

---

### 4. Content 组件

**职责**：显示主内容区域，处理加载状态和错误

**Props**：
```typescript
interface ContentProps {
  children: React.ReactNode;
  loading?: boolean;
  error?: string;
}
```

**功能**：
- 显示页面内容
- 处理加载状态（显示 Skeleton）
- 处理错误状态（显示错误信息）
- 响应式 padding 和 margin
- 支持滚动

---

## Data Models

### 1. Layout State (Zustand Store)

```typescript
interface LayoutState {
  // Sidebar 状态
  sidebarCollapsed: boolean;
  setSidebarCollapsed: (collapsed: boolean) => void;
  
  // 系统模式
  systemMode: 'admin' | 'app';
  setSystemMode: (mode: 'admin' | 'app') => void;
  
  // 视图模式
  viewMode: 'list' | 'card';
  setViewMode: (mode: 'list' | 'card') => void;
  
  // 主题
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;
  
  // 菜单数据
  menuItems: MenuItem[];
  setMenuItems: (items: MenuItem[]) => void;
  
  // 菜单加载状态
  menuLoading: boolean;
  setMenuLoading: (loading: boolean) => void;
  
  // 菜单错误
  menuError: string | null;
  setMenuError: (error: string | null) => void;
  
  // 初始化
  initializeLayout: () => void;
}
```

### 2. User State (Zustand Store)

```typescript
interface UserState {
  userInfo: {
    id: number;
    username: string;
    nickname: string;
    avatar?: string;
  } | null;
  
  permissions: string[];
  
  setUserInfo: (info: UserState['userInfo']) => void;
  setPermissions: (permissions: string[]) => void;
  
  hasPermission: (permission: string) => boolean;
  logout: () => void;
}
```

### 3. Menu Item Model

```typescript
interface MenuItem {
  id: string;
  label: string;
  icon?: string; // Icon name from Ant Design Icons
  path?: string; // Route path
  children?: MenuItem[];
  permissions?: string[]; // Required permissions
  visible?: boolean; // Whether to show this item
  order?: number; // Display order
}
```

### 4. API Response Models

```typescript
// 菜单数据 API 响应
interface MenuResponse {
  code: number;
  message: string;
  data: MenuItem[];
}

// 用户信息 API 响应
interface UserInfoResponse {
  code: number;
  message: string;
  data: {
    id: number;
    username: string;
    nickname: string;
    avatar?: string;
    permissions: string[];
  };
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property 1: System Mode Persistence Round Trip

*For any* system mode selection (admin or app), when the user selects a mode and the page is refreshed, the layout should restore the previously selected mode from local storage.

**Validates: Requirements 2.2, 2.4, 2.5**

### Property 2: View Mode Persistence Round Trip

*For any* view mode selection (list or card), when the user selects a mode and the page is refreshed, the layout should restore the previously selected mode from local storage.

**Validates: Requirements 3.3, 3.5, 3.6**

### Property 3: Sidebar Collapse State Persistence

*For any* sidebar collapse state, when the user toggles the collapse button and the page is refreshed, the layout should restore the previously saved collapse state from local storage.

**Validates: Requirements 6.2, 6.6, 6.7**

### Property 4: Menu Items Filtered by Permissions

*For any* user with a specific set of permissions, the sidebar should only display menu items that the user has permission to access, and should not display items requiring permissions the user lacks.

**Validates: Requirements 5.1, 7.2, 20.3**

### Property 5: Menu Item Navigation

*For any* menu item in the sidebar, when the user clicks on it, the layout should navigate to the corresponding route and highlight the active menu item.

**Validates: Requirements 5.3, 5.4**

### Property 6: Nested Menu Support

*For any* menu item with sub-menus, the sidebar should display an expand/collapse icon, and when expanded, should display all child menu items with proper indentation.

**Validates: Requirements 5.5, 5.6**

### Property 7: Logout Clears Session

*For any* logged-in user, when the user clicks the logout option, the layout should clear user session data, redirect to the login page, and clear all session-related local storage items.

**Validates: Requirements 4.2, 4.3, 4.4**

### Property 8: Content Area Responsive Layout

*For any* viewport width, the content area should adapt its layout appropriately: collapse sidebar on mobile (<768px), maintain sidebar on tablet (768-1024px), and expand fully on desktop (>1024px).

**Validates: Requirements 8.4, 9.1, 10.1, 11.1, 11.2**

### Property 9: Mobile Sidebar Auto-Collapse

*For any* viewport width less than 768px, the sidebar should automatically collapse to show only icons, and menu item labels should be hidden.

**Validates: Requirements 9.1, 6.3**

### Property 10: Theme Persistence Round Trip

*For any* theme selection (light or dark), when the user selects a theme and the page is refreshed, the layout should restore the previously selected theme from local storage.

**Validates: Requirements 18.2, 18.4, 18.5**

### Property 11: Layout Visibility Based on Route

*For any* route, the layout system should display the Header and Sidebar on protected routes (e.g., /system/users) and hide them on public routes (e.g., /login).

**Validates: Requirements 12.1, 12.2, 12.3, 12.4**

### Property 12: Menu Data Caching

*For any* menu fetch operation, when the menu data is fetched once and stored in the Zustand store, subsequent requests should use the cached data without making additional API calls.

**Validates: Requirements 7.4, 15.4**

### Property 13: Permission-Based Route Access

*For any* user attempting to access a route without required permissions, the layout should redirect to an access denied page and not render the protected content.

**Validates: Requirements 20.1, 20.2**

### Property 14: Dynamic Permission Updates

*For any* permission change, the layout should dynamically update the menu display and route access, reflecting the new permissions immediately.

**Validates: Requirements 7.3, 20.4**

### Property 15: Keyboard Navigation Support

*For any* interactive element in the layout, the user should be able to navigate using Tab key, activate using Enter key, and close dropdowns using Escape key.

**Validates: Requirements 16.1, 16.2, 16.3**

### Property 16: Semantic HTML Structure

*For any* layout component, the rendered HTML should use semantic elements (nav, header, main, aside) and include appropriate ARIA labels and roles for accessibility.

**Validates: Requirements 17.1, 17.2, 17.3, 17.4, 17.5**

### Property 17: Menu Error Handling and Recovery

*For any* menu load failure, the layout should display an error message and provide a retry button that, when clicked, attempts to reload the menu data.

**Validates: Requirements 19.1, 19.2, 19.3**

### Property 18: User State Preservation During Navigation

*For any* navigation between protected routes, the layout should preserve user state (user info, permissions, preferences) without requiring re-authentication.

**Validates: Requirements 12.5**

### Property 19: Collapsed Sidebar Tooltip Display

*For any* collapsed sidebar state, when the user hovers over a menu item icon, the layout should display a tooltip with the menu item label.

**Validates: Requirements 6.5**

### Property 20: System Mode Highlighting

*For any* system mode selection, the header should visually highlight the currently selected mode in the system mode selector dropdown.

**Validates: Requirements 2.3**

---

## Error Handling

### 1. Menu Loading Errors

**Scenario**: Menu data fails to load from the backend

**Handling Strategy**:
- Display an error message in the Sidebar
- Provide a retry button to reload menu data
- Log the error for debugging
- Fallback to cached menu data if available
- Set a reasonable timeout for menu loading (e.g., 5 seconds)

**Implementation**:
```typescript
// In layoutStore
const fetchMenuData = async () => {
  setMenuLoading(true);
  setMenuError(null);
  try {
    const response = await menuApi.getMenus();
    setMenuItems(response.data);
  } catch (error) {
    setMenuError('Failed to load menu data');
    console.error('Menu loading error:', error);
  } finally {
    setMenuLoading(false);
  }
};

const retryFetchMenuData = () => {
  fetchMenuData();
};
```

### 2. Permission Denied Errors

**Scenario**: User tries to access a route without permission

**Handling Strategy**:
- Redirect to an access denied page
- Display a message explaining the permission requirement
- Provide a link to return to the previous page
- Log the unauthorized access attempt

### 3. API Errors

**Scenario**: API calls fail (network error, server error, etc.)

**Handling Strategy**:
- Display user-friendly error messages
- Implement exponential backoff for retries
- Cache data when possible to provide offline functionality
- Log errors for monitoring and debugging

### 4. State Initialization Errors

**Scenario**: Failed to load state from local storage

**Handling Strategy**:
- Use default values for layout state
- Clear corrupted local storage data
- Log the error for debugging
- Notify user if necessary

---

## Testing Strategy

### Unit Testing

Unit tests focus on specific examples, edge cases, and error conditions:

1. **Header Component Tests**
   - Verify system name is displayed
   - Verify user info is displayed correctly
   - Test user dropdown menu interactions
   - Test system mode selector functionality
   - Test view mode selector functionality
   - Test theme toggle functionality

2. **Sidebar Component Tests**
   - Verify menu items are rendered based on permissions
   - Test menu item click navigation
   - Test collapse/expand functionality
   - Test tooltip display on hover
   - Test nested menu rendering

3. **Layout Component Tests**
   - Verify layout visibility based on route
   - Test responsive behavior at different viewport sizes
   - Test state persistence and restoration

4. **Store Tests**
   - Test state initialization from local storage
   - Test state updates and persistence
   - Test permission filtering logic

### Property-Based Testing

Property-based tests verify universal properties across all inputs using a PBT library (e.g., fast-check for JavaScript):

1. **System Mode Persistence** (Property 1)
   - Generate random system mode selections
   - Verify persistence and restoration from local storage
   - Minimum 100 iterations

2. **View Mode Persistence** (Property 2)
   - Generate random view mode selections
   - Verify persistence and restoration from local storage
   - Minimum 100 iterations

3. **Sidebar Collapse State** (Property 3)
   - Generate random collapse states
   - Verify persistence and restoration
   - Minimum 100 iterations

4. **Menu Filtering by Permissions** (Property 4)
   - Generate random permission sets and menu items
   - Verify only permitted items are displayed
   - Minimum 100 iterations

5. **Menu Navigation** (Property 5)
   - Generate random menu items and routes
   - Verify navigation and highlighting work correctly
   - Minimum 100 iterations

6. **Responsive Layout** (Property 8)
   - Generate random viewport widths
   - Verify layout adapts correctly at breakpoints
   - Minimum 100 iterations

7. **Theme Persistence** (Property 10)
   - Generate random theme selections
   - Verify persistence and restoration
   - Minimum 100 iterations

8. **Menu Caching** (Property 12)
   - Generate multiple menu fetch requests
   - Verify cached data is used without additional API calls
   - Minimum 100 iterations

9. **Permission-Based Access** (Property 13)
   - Generate random permission sets and routes
   - Verify access control works correctly
   - Minimum 100 iterations

10. **Keyboard Navigation** (Property 15)
    - Generate random keyboard inputs
    - Verify navigation and interactions work correctly
    - Minimum 100 iterations

### Test Configuration

- **Framework**: Vitest for unit tests, fast-check for property-based tests
- **Coverage Target**: >80% code coverage
- **Performance**: All tests should complete within 5 seconds
- **CI/CD**: Tests run on every commit

---

## Performance Considerations

### 1. Component Optimization

- Use React.memo for menu items to prevent unnecessary re-renders
- Implement useMemo for expensive computations
- Use useCallback for event handlers

### 2. State Management

- Minimize store subscriptions
- Use selector functions to subscribe only to needed state
- Implement store persistence efficiently

### 3. Menu Loading

- Implement pagination for large menu lists
- Cache menu data to avoid redundant API calls
- Display skeleton loaders during loading

### 4. Responsive Design

- Use CSS media queries instead of JavaScript for layout changes
- Implement debouncing for window resize events
- Lazy-load components for different screen sizes

### 5. Bundle Size

- Tree-shake unused Ant Design components
- Lazy-load route components
- Minimize CSS bundle size

---

## Integration Points

### 1. With React Router

- Use useLocation hook to detect route changes
- Highlight active menu item based on current route
- Implement route protection based on permissions

### 2. With Zustand Store

- Initialize store from local storage on app startup
- Subscribe to store changes for UI updates
- Persist store state to local storage on changes

### 3. With Ant Design

- Use Ant Design Layout, Header, Sider, Menu components
- Apply Ant Design theme customization
- Use Ant Design icons for menu items

### 4. With Backend API

- Fetch menu data based on user permissions
- Fetch user info on login
- Handle API errors gracefully

---

## File Structure

```
frontend/src/
├── components/
│   ├── layout/
│   │   ├── Layout.tsx
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   ├── Content.tsx
│   │   └── index.ts
│   └── ...
├── stores/
│   ├── layoutStore.ts
│   ├── userStore.ts
│   └── index.ts
├── services/
│   ├── menuApi.ts
│   ├── userApi.ts
│   └── index.ts
├── hooks/
│   ├── useLayout.ts
│   ├── useMenu.ts
│   └── index.ts
├── types/
│   ├── layout.ts
│   ├── menu.ts
│   └── index.ts
├── styles/
│   ├── layout.scss
│   ├── responsive.scss
│   └── theme.scss
├── App.tsx
└── main.tsx
```

---

## Next Steps

1. Implement Layout, Header, Sidebar, Content components
2. Create Zustand stores for layout and user state
3. Implement API services for menu and user data
4. Add responsive design with CSS media queries
5. Implement theme switching functionality
6. Add keyboard navigation and accessibility features
7. Write unit tests and property-based tests
8. Integrate with existing pages (LoginPage, UserPage)
9. Performance optimization and testing
10. Documentation and deployment
