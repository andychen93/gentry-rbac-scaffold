import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { StatusSwitch } from './StatusSwitch';

describe('StatusSwitch', () => {
  it('checked 状态由 status 决定', () => {
    const { rerender } = render(<StatusSwitch id={1} status={1} onToggle={vi.fn()} />);
    expect(screen.getByRole('switch').getAttribute('aria-checked')).toBe('true');
    rerender(<StatusSwitch id={1} status={0} onToggle={vi.fn()} />);
    expect(screen.getByRole('switch').getAttribute('aria-checked')).toBe('false');
  });
  it('点击调 onToggle(id, nextChecked)', () => {
    const onToggle = vi.fn().mockResolvedValue(undefined);
    render(<StatusSwitch id={5} status={1} onToggle={onToggle} />);
    fireEvent.click(screen.getByRole('switch'));
    expect(onToggle).toHaveBeenCalledWith(5, false);
  });
  it('disabled 时不可点', () => {
    render(<StatusSwitch id={1} status={1} onToggle={vi.fn()} disabled />);
    expect(screen.getByRole('switch')).toBeDisabled();
  });
});
