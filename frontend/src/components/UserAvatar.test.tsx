import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import UserAvatar from './UserAvatar';

describe('B3 UserAvatar', () => {
  test('shows the stored picture of the user', () => {
    render(<UserAvatar user={{ name: 'Ana', avatarId: 'abc-123' }} alt="Foto de Ana" />);

    expect(screen.getByAltText('Foto de Ana')).toHaveAttribute('src', '/api/v1/images/abc-123');
  });

  test('shows the initial in capitals when there is no avatar', () => {
    const { container } = render(<UserAvatar user={{ name: 'ana', avatarId: null }} />);

    expect(screen.getByText('A')).toBeInTheDocument();
    expect(container.querySelector('img')).toBeNull();
  });

  test('falls back to the initial when the picture fails to load', () => {
    render(<UserAvatar user={{ name: 'Ana', avatarId: 'gone' }} alt="Foto de Ana" />);

    fireEvent.error(screen.getByAltText('Foto de Ana'));

    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.queryByAltText('Foto de Ana')).not.toBeInTheDocument();
  });

  test('uses the small size by default and the large one on request', () => {
    const { container, rerender } = render(<UserAvatar user={{ name: 'Ana', avatarId: null }} />);
    expect(container.firstElementChild).toHaveClass('user-avatar--sm');

    rerender(<UserAvatar user={{ name: 'Ana', avatarId: null }} size="lg" />);

    expect(container.firstElementChild).toHaveClass('user-avatar--lg');
  });
});
