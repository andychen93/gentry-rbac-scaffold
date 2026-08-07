# 天地图集成 — 前端详细设计

> **版本**：v1.0 | **日期**：2026-05-11 | **状态**：待评审

---

## 1. 概述

### 1.1 目的

为车联网平台提供统一的地图能力，首期集成天地图（国家地理信息公共服务平台），支持实时监控与轨迹回放两大业务场景。

### 1.2 范围

| 场景 | 地图能力 |
|------|---------|
| 实时监控 | 设备 Marker 展示、InfoWindow 弹窗、车辆定位追踪 |
| 轨迹回放 | Polyline 绘制、起止点标注、轨迹动画、进度控制 |
| 未来扩展 | 电子围栏（Polygon）、热力图、聚类 |

### 1.3 设计原则

1. **Provider 抽象** — 地图操作通过 `MapProvider` 接口抽象，业务层不直接依赖天地图 API
2. **增量更新** — Marker 使用 diff 算法，避免全量销毁重建
3. **配置外置** — API Key 通过环境变量注入，不硬编码
4. **错误隔离** — 地图加载失败不阻塞页面其他功能

---

## 2. 技术选型

| 项目 | 选型 | 说明 |
|------|------|------|
| 地图 SDK | 天地图 JavaScript API v4.0 | 国家标准 CGCS2000，免费、合规 |
| 加载方式 | 动态 Script 标签 | 避免打包体积膨胀，按需加载 |
| 坐标系 | CGCS2000（≈WGS84） | 天地图与 GPS 原始坐标偏差 < 1m，车联网场景无需转换 |
| 状态管理 | Zustand（MapStore） | 地图实例 + 设备位置状态 |
| React 集成 | 自定义 Hook + useRef | 地图实例通过 ref 管理，避免 React 重渲染 |

### 2.1 为什么选天地图

| 对比项 | 天地图 | 高德 | 百度 |
|--------|--------|------|------|
| 坐标系 | CGCS2000 | GCJ-02（火星坐标） | BD-09 |
| GPS 偏差 | < 1m，无需转换 | 需 WGS84→GCJ-02 转换 | 需双重转换 |
| 费用 | 免费（有配额） | 免费额度 + 超量付费 | 免费额度 + 超量付费 |
| 合规性 | 国家测绘局 | 商业地图 | 商业地图 |
| API 风格 | 自有 API | AMap JSAPI | BMap JSAPI |

> 车联网设备上报 WGS84 坐标，天地图 CGCS2000 与 WGS84 偏差可忽略，省去坐标转换步骤。

---

## 3. 文件结构

```
frontend/src/
├── widgets/
│   └── map/
│       ├── types.ts                    # MapProvider 接口定义
│       ├── MapFactory.ts               # Provider 工厂
│       ├── MapLoader.ts                # Script 动态加载器
│       ├── MapContainer.tsx            # 通用地图容器组件
│       ├── MapErrorBoundary.tsx        # 地图错误边界
│       ├── providers/
│       │   └── TiandituProvider.ts     # 天地图实现
│       └── hooks/
│           ├── useMap.ts               # 地图实例 Hook
│           └── useTrackAnimator.ts     # 轨迹动画 Hook
├── stores/
│   └── mapStore.ts                     # 地图状态 Store
└── utils/
    └── coordConvert.ts                 # 坐标转换工具（预留）
```

---

## 4. 核心接口设计

### 4.1 MapProvider 接口

```typescript
// widgets/map/types.ts

export interface MapOptions {
  center: [number, number];   // [经度, 纬度]
  zoom: number;
  minZoom?: number;           // 默认 3
  maxZoom?: number;           // 默认 18
}

export interface MarkerOptions {
  id: string;
  position: [number, number];
  icon?: string;
  iconSize?: [number, number];
  iconAnchor?: [number, number];
  label?: string;
  labelOffset?: [number, number];
  rotation?: number;          // 方向角度
  data?: Record<string, any>;
}

export interface PolylineOptions {
  color?: string;             // 默认 #1890ff
  weight?: number;            // 默认 3
  opacity?: number;           // 默认 1
}

export interface Marker {
  setPosition(lng: number, lat: number): void;
  setIcon(icon: string, size?: [number, number]): void;
  setLabel(text: string, offset?: [number, number]): void;
  setRotation(angle: number): void;
  setData(data: Record<string, any>): void;
  show(): void;
  hide(): void;
  on(event: string, handler: (e: any) => void): void;
  off(event: string, handler: (e: any) => void): void;
}

export interface Polyline {
  setPoints(points: [number, number][]): void;
  setColor(color: string): void;
  show(): void;
  hide(): void;
}

export interface InfoWindowOptions {
  content: string | HTMLElement;
  position: [number, number];
  offset?: [number, number];
}

export interface MapProvider {
  readonly type: string;

  // 生命周期
  init(container: HTMLElement, options: MapOptions): void;
  destroy(): void;

  // 视图控制
  setCenter(lng: number, lat: number): void;
  getCenter(): [number, number];
  setZoom(zoom: number): void;
  getZoom(): number;
  fitBounds(bounds: [[number, number], [number, number]]): void;

  // 覆盖物
  addMarker(options: MarkerOptions): Marker;
  removeMarker(marker: Marker): void;
  updateMarker(marker: Marker, options: Partial<MarkerOptions>): void;
  addPolyline(points: [number, number][], options?: PolylineOptions): Polyline;
  removePolyline(polyline: Polyline): void;
  openInfoWindow(options: InfoWindowOptions): void;
  closeInfoWindow(): void;
  clearAll(): void;

  // 事件
  on(event: 'click' | 'zoomend' | 'moveend', handler: (e: any) => void): void;
  off(event: string, handler: (e: any) => void): void;
}
```

### 4.2 MapProvider 工厂

```typescript
// widgets/map/MapFactory.ts

import type { MapProvider } from './types';
import { TiandituProvider } from './providers/TiandituProvider';

const PROVIDERS: Record<string, new () => MapProvider> = {
  tianditu: TiandituProvider,
  // amap: AMapProvider,    // 未来扩展
  // bmap: BMapProvider,    // 未来扩展
};

export function createMapProvider(type: string): MapProvider {
  const Provider = PROVIDERS[type];
  if (!Provider) {
    throw new Error(`未知的地图提供商: ${type}`);
  }
  return new Provider();
}
```

---

## 5. 天地图 Provider 实现

### 5.1 Script 加载器

```typescript
// widgets/map/MapLoader.ts

const TIANDITU_URL = 'https://api.tianditu.gov.cn/api?v=4.0&tk=';

let loadPromise: Promise<void> | null = null;

export function loadTianditu(apiKey: string): Promise<void> {
  if ((window as any).T) {
    return Promise.resolve();
  }

  if (loadPromise) {
    return loadPromise;
  }

  loadPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `${TIANDITU_URL}${apiKey}`;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('天地图 SDK 加载失败'));
    document.head.appendChild(script);
  });

  return loadPromise;
}
```

### 5.2 TiandituProvider

```typescript
// widgets/map/providers/TiandituProvider.ts

import type {
  MapProvider, MapOptions, MarkerOptions, Marker,
  PolylineOptions, Polyline, InfoWindowOptions,
} from '../types';

declare const T: any;

export class TiandituProvider implements MapProvider {
  readonly type = 'tianditu';

  private map: any = null;
  private infoWindow: any = null;

  // ========== 生命周期 ==========

  init(container: HTMLElement, options: MapOptions): void {
    this.map = new T.Map(container, {
      projection: 'EPSG:900913',
    });
    this.map.centerAndZoom(
      new T.LngLat(options.center[0], options.center[1]),
      options.zoom,
    );

    this.map.addControl(new T.Control.Zoom());
    this.map.addControl(new T.Control.Scale());

    // InfoWindow 复用
    this.infoWindow = new T.InfoWindow();
  }

  destroy(): void {
    if (this.map) {
      this.map.clearOverLays();
      this.map = null;
    }
  }

  // ========== 视图控制 ==========

  setCenter(lng: number, lat: number): void {
    this.map.panTo(new T.LngLat(lng, lat));
  }

  getCenter(): [number, number] {
    const c = this.map.getCenter();
    return [c.lng, c.lat];
  }

  setZoom(zoom: number): void {
    this.map.setZoom(zoom);
  }

  getZoom(): number {
    return this.map.getZoom();
  }

  fitBounds(bounds: [[number, number], [number, number]]): void {
    const viewport = this.map.getViewport([
      new T.LngLat(bounds[0][0], bounds[0][1]),
      new T.LngLat(bounds[1][0], bounds[1][1]),
    ]);
    this.map.centerAndZoom(viewport.center, viewport.zoom);
  }

  // ========== 覆盖物 ==========

  addMarker(options: MarkerOptions): Marker {
    const point = new T.LngLat(options.position[0], options.position[1]);
    let rawMarker: any;

    if (options.icon) {
      const icon = new T.Icon({
        iconUrl: options.icon,
        iconSize: options.iconSize
          ? new T.Point(options.iconSize[0], options.iconSize[1])
          : undefined,
        iconAnchor: options.iconAnchor
          ? new T.Point(options.iconAnchor[0], options.iconAnchor[1])
          : undefined,
      });
      rawMarker = new T.Marker(point, { icon });
    } else {
      rawMarker = new T.Marker(point);
    }

    if (options.label) {
      const label = new T.Label({
        text: options.label,
        position: point,
        offset: options.labelOffset
          ? new T.Point(options.labelOffset[0], options.labelOffset[1])
          : new T.Point(0, -30),
      });
      this.map.addOverLay(label);
    }

    if (options.rotation !== undefined) {
      rawMarker.setRotation(options.rotation);
    }

    if (options.data) {
      rawMarker._data = options.data;
    }

    this.map.addOverLay(rawMarker);

    return this.wrapMarker(rawMarker);
  }

  removeMarker(marker: Marker): void {
    const raw = (marker as any)._raw;
    this.map.removeOverLay(raw);
  }

  updateMarker(marker: Marker, options: Partial<MarkerOptions>): void {
    const raw = (marker as any)._raw;
    if (options.position) {
      raw.setLngLat(new T.LngLat(options.position[0], options.position[1]));
    }
    if (options.rotation !== undefined) {
      raw.setRotation(options.rotation);
    }
    if (options.icon) {
      const icon = new T.Icon({ iconUrl: options.icon });
      raw.setIcon(icon);
    }
    if (options.data) {
      raw._data = options.data;
    }
  }

  addPolyline(points: [number, number][], options?: PolylineOptions): Polyline {
    const lngLats = points.map((p) => new T.LngLat(p[0], p[1]));
    const polyline = new T.Polyline(lngLats, {
      color: options?.color ?? '#5E72E4',
      weight: options?.weight ?? 4,
      opacity: options?.opacity ?? 1,
    });
    this.map.addOverLay(polyline);
    return this.wrapPolyline(polyline);
  }

  removePolyline(polyline: Polyline): void {
    const raw = (polyline as any)._raw;
    this.map.removeOverLay(raw);
  }

  openInfoWindow(options: InfoWindowOptions): void {
    const position = new T.LngLat(options.position[0], options.position[1]);
    this.infoWindow.setContent(options.content);
    this.infoWindow.setLngLat(position);
    this.infoWindow.open(this.map);
  }

  closeInfoWindow(): void {
    this.infoWindow.close();
  }

  clearAll(): void {
    this.map.clearOverLays();
  }

  // ========== 事件 ==========

  on(event: string, handler: (e: any) => void): void {
    this.map.addEventListener(event, handler);
  }

  off(event: string, handler: (e: any) => void): void {
    this.map.removeEventListener(event, handler);
  }

  // ========== 内部包装 ==========

  private wrapMarker(raw: any): Marker {
    return {
      setPosition: (lng, lat) => raw.setLngLat(new T.LngLat(lng, lat)),
      setIcon: (icon, size) => {
        const iconObj = new T.Icon({
          iconUrl: icon,
          iconSize: size ? new T.Point(size[0], size[1]) : undefined,
        });
        raw.setIcon(iconObj);
      },
      setLabel: (text, offset) => {
        const label = new T.Label({
          text,
          offset: offset ? new T.Point(offset[0], offset[1]) : undefined,
        });
        raw.setLabel(label);
      },
      setRotation: (angle) => raw.setRotation(angle),
      setData: (data) => { raw._data = data; },
      show: () => raw.show(),
      hide: () => raw.hide(),
      on: (evt, handler) => raw.addEventListener(evt, handler),
      off: (evt, handler) => raw.removeEventListener(evt, handler),
      _raw: raw,
    } as Marker & { _raw: any };
  }

  private wrapPolyline(raw: any): Polyline {
    return {
      setPoints: (points) => {
        const lngLats = points.map((p) => new T.LngLat(p[0], p[1]));
        raw.setLngLats(lngLats);
      },
      setColor: (color) => raw.setColor(color),
      show: () => raw.show(),
      hide: () => raw.hide(),
      _raw: raw,
    } as Polyline & { _raw: any };
  }
}
```

---

## 6. MapStore 状态设计

```typescript
// stores/mapStore.ts

import { create } from 'zustand';

interface DeviceLocation {
  deviceId: string;
  deviceSn: string;
  plateNumber: string | null;
  longitude: number;
  latitude: number;
  speed: number;
  direction: number;
  onlineStatus: number;
  lastOnlineTime: string;
}

interface MapState {
  // 地图 Provider 类型
  providerType: string;

  // 设备位置数据（监控用）
  deviceLocations: Map<string, DeviceLocation>;

  // 选中设备
  selectedDeviceId: string | null;

  // 地图就绪状态
  mapReady: boolean;

  // Actions
  setProviderType: (type: string) => void;
  setDeviceLocations: (locations: DeviceLocation[]) => void;
  updateDeviceLocation: (location: DeviceLocation) => void;
  selectDevice: (deviceId: string | null) => void;
  setMapReady: (ready: boolean) => void;
}

export const useMapStore = create<MapState>((set) => ({
  providerType: 'tianditu',
  deviceLocations: new Map(),
  selectedDeviceId: null,
  mapReady: false,

  setProviderType: (type) => set({ providerType: type }),

  setDeviceLocations: (locations) => {
    const map = new Map<string, DeviceLocation>();
    locations.forEach((loc) => map.set(loc.deviceId, loc));
    set({ deviceLocations: map });
  },

  updateDeviceLocation: (location) =>
    set((state) => {
      const map = new Map(state.deviceLocations);
      map.set(location.deviceId, location);
      return { deviceLocations: map };
    }),

  selectDevice: (deviceId) => set({ selectedDeviceId: deviceId }),

  setMapReady: (ready) => set({ mapReady: ready }),
}));
```

---

## 7. 组件设计

### 7.1 MapContainer — 通用地图容器

**职责**：初始化地图、管理 Provider 实例、提供 `mapRef` 给子组件。

```typescript
// widgets/map/MapContainer.tsx

interface MapContainerProps {
  /** 地图容器高度，默认 100% */
  height?: number | string;
  /** 初始中心点，默认杭州 [120.15, 30.28] */
  defaultCenter?: [number, number];
  /** 初始缩放，默认 6 */
  defaultZoom?: number;
  /** 地图加载完成回调 */
  onMapReady?: (provider: MapProvider) => void;
  /** 子组件通过 children 渲染覆盖层 */
  children?: React.ReactNode;
  /** 自定义 className */
  className?: string;
}
```

**核心逻辑**：

1. `useEffect` 中调用 `loadTianditu(apiKey)` 加载 SDK
2. SDK 加载后调用 `createMapProvider('tianditu')` 创建 Provider
3. 调用 `provider.init(container, options)` 初始化地图
4. 通过 `onMapReady` 回调将 Provider 传递给父组件
5. 组件卸载时调用 `provider.destroy()`

### 7.2 useMap Hook

```typescript
// widgets/map/hooks/useMap.ts

export function useMap() {
  const providerRef = useRef<MapProvider | null>(null);

  const init = useCallback((container: HTMLElement, options: MapOptions) => {
    const apiKey = import.meta.env.VITE_TIANDITU_KEY;
    loadTianditu(apiKey).then(() => {
      const provider = createMapProvider('tianditu');
      provider.init(container, options);
      providerRef.current = provider;
    });
  }, []);

  const getProvider = useCallback(() => providerRef.current, []);

  // 组件卸载时自动销毁
  useEffect(() => {
    return () => {
      providerRef.current?.destroy();
      providerRef.current = null;
    };
  }, []);

  return { init, getProvider };
}
```

### 7.3 useTrackAnimator Hook — 轨迹动画

```typescript
// widgets/map/hooks/useTrackAnimator.ts

interface TrackAnimatorOptions {
  provider: MapProvider;
  points: [number, number][];
  speed: 1 | 2 | 4 | 8;           // 倍速
  interval: number;                  // 毫秒，默认 200
  onMove?: (index: number, point: [number, number]) => void;
  onFinish?: () => void;
}

interface TrackAnimator {
  play: () => void;
  pause: () => void;
  reset: () => void;
  setSpeed: (speed: 1 | 2 | 4 | 8) => void;
  seekTo: (percent: number) => void;  // 0~1
  currentIndex: number;
  isPlaying: boolean;
}
```

**动画策略**：定时器按 `interval / speed` 间隔逐点移动 Marker，两点之间线性插值实现平滑过渡。

### 7.4 MapErrorBoundary

```typescript
// widgets/map/MapErrorBoundary.tsx

export class MapErrorBoundary extends Component<Props, State> {
  // 捕获地图初始化错误、API 加载失败
  // 渲染降级 UI：提示 + 重试按钮
}
```

---

## 8. 业务场景集成

### 8.1 实时监控页（MonitorPage）改造

**现有**：纯表格展示，地图区域为 placeholder div。

**改造后布局**：

```
┌──────────────────────────────────────────────────────┐
│  统计卡片行（总数/在线/离线/在线率）                        │
├──────────────┬───────────────────────────────────────┤
│  设备列表     │  MapContainer                          │
│  (左侧面板)   │  - 在线设备: 绿色车标                     │
│  - 搜索       │  - 离线设备: 灰色车标                     │
│  - 状态筛选    │  - 超速设备: 红色车标                     │
│  - 表格       │  - 点击 Marker → InfoWindow              │
│              │  - 点击表格行 → 地图定位                   │
└──────────────┴───────────────────────────────────────┘
```

**关键交互**：

| 操作 | 行为 |
|------|------|
| 表格点击某设备 | `selectDevice(id)` → 地图 `setCenter` + `setZoom(15)` → 打开 InfoWindow |
| 地图点击 Marker | 读取 `_data` → 渲染 InfoWindow（车牌、速度、时间、位置） |
| 10s 轮询刷新 | `setDeviceLocations` → Marker Diff 算法增量更新 |
| 状态筛选变化 | 过滤设备 → `marker.show()/hide()` |

**InfoWindow 内容模板**：

```tsx
<div style={{ padding: 8, minWidth: 200 }}>
  <div style={{ fontWeight: 600, fontSize: 14 }}>
    {plateNumber}
  </div>
  <div>速度: {speed} km/h</div>
  <div>方向: {direction}°</div>
  <div>时间: {lastOnlineTime}</div>
  <div>状态: {onlineStatus === 1 ? '在线' : '离线'}</div>
</div>
```

### 8.2 轨迹回放页（TrackPage）改造

**现有**：查询面板 + placeholder div + 数据表格。

**改造后布局**：

```
┌──────────────────────────────────────────────────────┐
│  查询面板（设备SN + 时间范围 + 查询按钮）                  │
├──────────────────────────────────────────────────────┤
│  MapContainer                                        │
│  - Polyline（轨迹线，蓝色 #5E72E4）                     │
│  - 起点标记（绿色圆点）                                  │
│  - 终点标记（红色圆点）                                  │
│  - 动画 Marker（车辆图标，沿轨迹移动）                     │
├──────────────────────────────────────────────────────┤
│  回放控制栏                                            │
│  [◀] [▶播放] [▶▶]  ━━━━●━━━━  1x ▼  进度: 45/230点    │
├──────────────────────────────────────────────────────┤
│  轨迹数据表格（时间/经度/纬度/速度/方向/ACC/定位/里程）      │
└──────────────────────────────────────────────────────┘
```

**轨迹绘制流程**：

1. 查询位置数据 → 后端返回 `LocationRecord[]`
2. 创建 Polyline：`provider.addPolyline(points, { color: '#5E72E4', weight: 4 })`
3. 创建起止点 Marker：绿色起点 + 红色终点
4. `fitBounds` 自动调整视野包含整条轨迹
5. 创建动画 Marker（隐藏状态）
6. 用户点击播放 → `useTrackAnimator` 驱动动画

**回放控制**：

| 控件 | 功能 |
|------|------|
| 播放/暂停 | 切换 `TrackAnimator.play()/pause()` |
| 倍速 | `setSpeed(1/2/4/8)` |
| 进度条 | `seekTo(percent)` |
| 重置 | `reset()` → Marker 回到起点 |

---

## 9. API Key 管理

### 9.1 环境变量

```bash
# .env.development
VITE_TIANDITU_KEY=your_development_key

# .env.production
VITE_TIANDITU_KEY=your_production_key
```

### 9.2 多租户扩展

当前采用统一 Key（环境变量），未来可按租户配置不同 Key：

```typescript
// 从 tenantConfig 读取 mapProvider 和 apiKey
const config = useTenantStore((s) => s.config);
const apiKey = config?.mapApiKey ?? import.meta.env.VITE_TIANDITU_KEY;
```

租户配置表中 `map_provider` 和 `map_api_key` 字段已预留（见 TenantConfigModal 中的 mapProvider 选项）。

---

## 10. 坐标系处理

### 10.1 当前策略

天地图使用 CGCS2000 坐标系，与 GPS 设备上报的 WGS84 坐标偏差 < 1 米，车联网场景下**无需转换**，直接使用。

### 10.2 预留转换工具

`utils/coordConvert.ts` 提供 WGS84 ↔ GCJ-02 ↔ BD-09 双向转换函数，供未来接入高德/百度地图时使用。

| 转换方向 | 函数 | 使用场景 |
|---------|------|---------|
| WGS84 → GCJ-02 | `wgs84ToGcj02()` | 接入高德地图 |
| GCJ-02 → WGS84 | `gcj02ToWgs84()` | 高德坐标还原 |
| WGS84 → BD-09 | `wgs84ToBd09()` | 接入百度地图 |
| BD-09 → WGS84 | `bd09ToWgs84()` | 百度坐标还原 |

### 10.3 Provider 内置转换

未来 AMapProvider / BMapProvider 在 `addMarker` / `addPolyline` 内部自动调用 `coordConvert` 转换坐标，业务层无需感知。

---

## 11. 性能考量

### 11.1 Marker Diff 算法

实时监控场景下，设备位置每 10 秒轮询一次。避免全量 `clearOverLays + addMarker`，使用增量 diff：

```
新数据到达 → diff(newIds, existingIds)
  - 新增的设备 → addMarker
  - 消失的设备 → removeMarker
  - 已存在的设备 → updateMarker (仅更新位置/方向/图标)
```

### 11.2 大数据量优化

| 设备数量 | 策略 |
|---------|------|
| < 500 | 直接渲染 Marker |
| 500~5000 | 聚类（MarkerCluster），按缩放级别聚合 |
| > 5000 | 热力图替代 Marker |

### 11.3 轨迹点抽稀

轨迹数据可能包含数千个点，绘制前进行 Douglas-Peucker 抽稀：

- 缩放级别 < 10：抽稀至 200 点
- 缩放级别 10~14：抽稀至 500 点
- 缩放级别 ≥ 15：全量绘制

### 11.4 地图实例复用

单页面只创建一个 MapProvider 实例，通过 `useRef` 持有，组件重渲染不影响地图实例。

---

## 12. 错误处理

| 场景 | 处理 |
|------|------|
| SDK 加载失败 | MapErrorBoundary 捕获，显示降级 UI + 重试按钮 |
| API Key 无效 | 天地图 SDK 返回错误弹窗，前端 console.error 提示 |
| 地图容器未挂载 | `useMap` 内判断 container 非空才初始化 |
| 网络断开 | 地图瓦片加载失败显示空白，恢复后自动重载 |
| 坐标数据异常 | 经纬度范围校验（lng: 73~136, lat: 3~54） |

---

## 13. 开发任务清单

| # | 任务 | 文件 | 预估 |
|---|------|------|------|
| 1 | 创建 MapProvider 接口定义 | `widgets/map/types.ts` | 0.5h |
| 2 | 实现 TiandituProvider | `widgets/map/providers/TiandituProvider.ts` | 2h |
| 3 | 实现 MapLoader + MapFactory | `widgets/map/MapLoader.ts`, `MapFactory.ts` | 0.5h |
| 4 | 实现 MapErrorBoundary | `widgets/map/MapErrorBoundary.tsx` | 0.5h |
| 5 | 实现 MapContainer 组件 | `widgets/map/MapContainer.tsx` | 1h |
| 6 | 实现 useMap Hook | `widgets/map/hooks/useMap.ts` | 0.5h |
| 7 | 创建 MapStore | `stores/mapStore.ts` | 0.5h |
| 8 | 坐标转换工具 | `utils/coordConvert.ts` | 0.5h |
| 9 | 改造 MonitorPage 集成地图 | `pages/monitorLive/MonitorPage.tsx` | 2h |
| 10 | 实现 useTrackAnimator Hook | `widgets/map/hooks/useTrackAnimator.ts` | 1.5h |
| 11 | 改造 TrackPage 集成地图 | `pages/track/TrackPage.tsx` | 2h |
| 12 | 环境变量配置 | `.env.development`, `.env.production` | 0.2h |
| **总计** | | | **~12h** |

---

## 14. 测试要点

| 测试项 | 方法 |
|--------|------|
| TiandituProvider 接口覆盖 | 单元测试，mock `window.T` |
| Marker Diff 算法正确性 | 单元测试，模拟新旧数据 diff |
| MapContainer 挂载/卸载 | React Testing Library，验证 init/destroy 调用 |
| MapErrorBoundary 降级 | 模拟 SDK 加载失败，验证降级 UI |
| 轨迹动画播放/暂停/倍速 | 单元测试，验证 Timer 行为 |
| 坐标转换精度 | 单元测试，已知坐标对验证 |
| MonitorPage 集成 | 手动测试，验证 Marker 展示和点击交互 |
| TrackPage 集成 | 手动测试，验证轨迹绘制和回放控制 |
