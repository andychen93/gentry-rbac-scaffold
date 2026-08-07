import { describe, it, expect, beforeEach } from 'vitest';
import { useLayoutStore, LAYOUT_SIDEBAR_PINNED_KEY } from './layoutStore';

describe('layoutStore sidebarPinned', () => {
  beforeEach(() => {
    localStorage.clear();
    useLayoutStore.setState({ sidebarPinned: true });
  });

  it('默认 pinned=true', () => {
    useLayoutStore.getState().setSidebarPinned(true);
    expect(useLayoutStore.getState().sidebarPinned).toBe(true);
  });

  it('toggleSidebarPinned 翻转并持久化', () => {
    useLayoutStore.setState({ sidebarPinned: true });
    useLayoutStore.getState().toggleSidebarPinned();
    expect(useLayoutStore.getState().sidebarPinned).toBe(false);
    expect(localStorage.getItem(LAYOUT_SIDEBAR_PINNED_KEY)).toBe('false');
  });

  it('initializeLayout 读取持久化值', () => {
    localStorage.setItem(LAYOUT_SIDEBAR_PINNED_KEY, 'false');
    useLayoutStore.getState().initializeLayout();
    expect(useLayoutStore.getState().sidebarPinned).toBe(false);
  });
});
