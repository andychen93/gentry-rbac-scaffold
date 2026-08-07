import React, { Suspense } from 'react';
import { Spin } from 'antd';

/** 缓存 React.lazy 组件，避免重复创建 */
const lazyCache = new Map<string, React.LazyExoticComponent<React.ComponentType>>();

function getLazyComponent(
  loader: () => Promise<{ default: React.ComponentType }>,
  key: string,
) {
  if (!lazyCache.has(key)) {
    lazyCache.set(key, React.lazy(loader));
  }
  return lazyCache.get(key)!;
}

interface LazyPageProps {
  loader: () => Promise<{ default: React.ComponentType }>;
  componentKey: string;
}

const LazyPage: React.FC<LazyPageProps> = ({ loader, componentKey }) => {
  const LazyComponent = getLazyComponent(loader, componentKey);

  return (
    <Suspense
      fallback={
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      }
    >
      <LazyComponent />
    </Suspense>
  );
};

export default LazyPage;
