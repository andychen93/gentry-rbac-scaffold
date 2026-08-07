import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AppFooter from './AppFooter';
import { APP_NAME } from '../../config/app';

describe('AppFooter', () => {
  it('渲染版权文案', () => {
    render(<AppFooter />);
    expect(screen.getByText(new RegExp(APP_NAME))).toBeInTheDocument();
    expect(screen.getByText(/©/)).toBeInTheDocument();
  });
});
