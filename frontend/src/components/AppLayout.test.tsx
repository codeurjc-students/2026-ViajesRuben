import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { test, expect } from 'vitest';
import AppLayout from './AppLayout';

test('wraps the page of the route between the header and the footer', () => {
  render(
    <MemoryRouter initialEntries={['/x']}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/x" element={<p>Contenido</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  expect(screen.getByRole('navigation')).toBeInTheDocument();
  expect(screen.getByText('Contenido')).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toBeInTheDocument();
});
