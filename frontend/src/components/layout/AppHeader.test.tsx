import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppHeader from './AppHeader';
import { useLayoutStore } from '../../stores/layoutStore';

const renderHeader = () => render(<AppHeader />, { wrapper: MemoryRouter });

describe('AppHeader', () => {
  it('折叠钮点击调 toggleSidebarPinned', () => {
    useLayoutStore.setState({ sidebarPinned: true });
    renderHeader();
    const btn = screen.getByLabelText('收起侧边栏');
    fireEvent.click(btn);
    expect(useLayoutStore.getState().sidebarPinned).toBe(false);
  });

  it('顶栏应用深青 class', () => {
    const { container } = renderHeader();
    expect(container.querySelector('.ps-header')).toBeInTheDocument();
  });
});
