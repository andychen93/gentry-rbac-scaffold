import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import PageShell from './PageShell';
import { useLayoutStore } from '../../stores/layoutStore';

describe('PageShell', () => {
  it('pinned=true → data-pinned true', () => {
    useLayoutStore.setState({ sidebarPinned: true });
    const { container } = render(<PageShell><div>x</div></PageShell>);
    expect(container.firstChild).toHaveAttribute('data-pinned', 'true');
  });
  it('pinned=false → data-pinned false', () => {
    useLayoutStore.setState({ sidebarPinned: false });
    const { container } = render(<PageShell><div>x</div></PageShell>);
    expect(container.firstChild).toHaveAttribute('data-pinned', 'false');
  });
  it('有 title 渲染两级标题', () => {
    useLayoutStore.setState({ sidebarPinned: true });
    render(<PageShell title="实时监控" subTitle="概览"><div>x</div></PageShell>);
    expect(screen.getByText('实时监控')).toBeInTheDocument();
    expect(screen.getByText('概览')).toBeInTheDocument();
  });
});
