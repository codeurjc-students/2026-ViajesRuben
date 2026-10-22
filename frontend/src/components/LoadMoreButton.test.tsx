import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import LoadMoreButton from './LoadMoreButton';

test('shows the button and calls the handler when clicked', () => {
  const onClick = vi.fn();
  render(<LoadMoreButton last={false} loading={false} onClick={onClick} />);

  fireEvent.click(screen.getByRole('button', { name: 'Más resultados' }));

  expect(onClick).toHaveBeenCalledTimes(1);
});

test('is hidden when there are no more pages', () => {
  render(<LoadMoreButton last loading={false} onClick={vi.fn()} />);

  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

test('is disabled with a spinner while a page is loading', () => {
  const onClick = vi.fn();
  render(<LoadMoreButton last={false} loading onClick={onClick} />);

  const button = screen.getByRole('button', { name: 'Más resultados' });
  fireEvent.click(button);

  expect(button).toBeDisabled();
  expect(screen.getByRole('status', { hidden: true })).toBeInTheDocument();
  expect(onClick).not.toHaveBeenCalled();
});
