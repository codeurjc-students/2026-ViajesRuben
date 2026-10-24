import { fireEvent, render, screen } from '@testing-library/react';
import { expect, test } from 'vitest';
import { DEFAULT_TRIP_COVER } from '../constants/images';
import StoredImage from './StoredImage';

test('shows the stored image of the given id', () => {
  render(<StoredImage id="abc-123" alt="Portada" />);

  expect(screen.getByAltText('Portada')).toHaveAttribute('src', '/api/v1/images/abc-123');
});

test('shows the default cover when there is no id', () => {
  render(<StoredImage alt="Portada" />);

  expect(screen.getByAltText('Portada')).toHaveAttribute('src', DEFAULT_TRIP_COVER);
});

test('falls back to the default cover when the image fails to load', () => {
  render(<StoredImage id="gone" alt="Portada" />);

  fireEvent.error(screen.getByAltText('Portada'));

  expect(screen.getByAltText('Portada')).toHaveAttribute('src', DEFAULT_TRIP_COVER);
});

test('tries the stored image again when the id changes after a failure', () => {
  const { rerender } = render(<StoredImage id="gone" alt="Portada" />);
  fireEvent.error(screen.getByAltText('Portada'));

  rerender(<StoredImage id="other" alt="Portada" />);

  expect(screen.getByAltText('Portada')).toHaveAttribute('src', '/api/v1/images/other');
});

test('does not loop if the default cover itself fails', () => {
  render(<StoredImage alt="Portada" />);

  fireEvent.error(screen.getByAltText('Portada'));

  expect(screen.getByAltText('Portada')).toHaveAttribute('src', DEFAULT_TRIP_COVER);
});

test('is decorative by default', () => {
  const { container } = render(<StoredImage />);

  expect(container.querySelector('img')).toHaveAttribute('alt', '');
});

test('B3 shows the fallback instead of the default cover when there is no id', () => {
  render(<StoredImage alt="Foto" fallback={<span>A</span>} />);

  expect(screen.getByText('A')).toBeInTheDocument();
  expect(screen.queryByAltText('Foto')).not.toBeInTheDocument();
});

test('B3 shows the fallback when the image fails to load', () => {
  render(<StoredImage id="gone" alt="Foto" fallback={<span>A</span>} />);

  fireEvent.error(screen.getByAltText('Foto'));

  expect(screen.getByText('A')).toBeInTheDocument();
});

test('B3 prefers the stored image over the fallback', () => {
  render(<StoredImage id="abc-123" alt="Foto" fallback={<span>A</span>} />);

  expect(screen.getByAltText('Foto')).toHaveAttribute('src', '/api/v1/images/abc-123');
  expect(screen.queryByText('A')).not.toBeInTheDocument();
});
