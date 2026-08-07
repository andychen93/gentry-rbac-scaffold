import '@testing-library/jest-dom';

// antd Menu / 弹出层依赖 ResizeObserver 与 matchMedia，jsdom 缺，补桩
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
(globalThis as any).ResizeObserver ||= ResizeObserverStub;
if (!window.matchMedia) {
  window.matchMedia = ((query: string) => ({
    matches: false, media: query, onchange: null,
    addEventListener() {}, removeEventListener() {},
    addListener() {}, removeListener() {},
    dispatchEvent: () => false,
  })) as any;
}
