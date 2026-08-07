import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AppLayout from './AppLayout';

describe('AppLayout', () => {
  it('渲染 PrecisionSidenav + PageShell + AppFooter', () => {
    render(
      <AppLayout>
        <div data-testid="page">内容</div>
      </AppLayout>,
      { wrapper: MemoryRouter },
    );
    expect(document.querySelector('.ps-sidenav')).toBeInTheDocument();
    expect(document.querySelector('.ps-page')).toBeInTheDocument();
    expect(document.querySelector('.ps-footer')).toBeInTheDocument();
    expect(screen.getByTestId('page')).toBeInTheDocument();
  });
});
