import { useEffect } from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import { vi, test, expect, describe, beforeEach } from 'vitest';
import AuthProvider from './AuthProvider';
import { useAuth } from '../hooks/useAuth';
import * as authService from '../services/authService';
import { ANA } from '../testHelpers';
import type { AuthContextValue } from '../context/AuthContext';

vi.mock('../services/authService');

const latest: { value: AuthContextValue | null } = { value: null };

/** The context value the provider exposed in its last render. */
function currentAuth(): AuthContextValue {
  if (!latest.value) throw new Error('The probe has not rendered');
  return latest.value;
}

function Probe() {
  const auth = useAuth();
  useEffect(() => {
    latest.value = auth;
  });
  return <p>{auth.loading ? 'cargando' : (auth.user?.name ?? 'nadie')}</p>;
}

function renderProvider() {
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );
}

beforeEach(() => {
  vi.resetAllMocks();
});

describe('B2 AuthProvider', () => {
  test('asks who is logged in on load and exposes that user', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(ANA);

    renderProvider();

    expect(screen.getByText('cargando')).toBeInTheDocument();
    expect(await screen.findByText('Ana')).toBeInTheDocument();
  });

  test('exposes nobody when there is no session', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(null);

    renderProvider();

    expect(await screen.findByText('nadie')).toBeInTheDocument();
  });

  test('treats a failed session check as logged out', async () => {
    vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('Network error'));

    renderProvider();

    expect(await screen.findByText('nadie')).toBeInTheDocument();
  });

  test('login keeps the user returned by the API', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(null);
    vi.mocked(authService.login).mockResolvedValue(ANA);
    renderProvider();
    await screen.findByText('nadie');

    await act(() => currentAuth().login({ email: ANA.email, password: 'secret-pass' }));

    expect(await screen.findByText('Ana')).toBeInTheDocument();
  });

  test('a failed login leaves nobody logged in and rejects', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(null);
    vi.mocked(authService.login).mockRejectedValue(new Error('401'));
    renderProvider();
    await screen.findByText('nadie');

    await expect(currentAuth().login({ email: 'a@b.es', password: 'x' })).rejects.toThrow('401');
    expect(screen.getByText('nadie')).toBeInTheDocument();
  });

  test('register creates the account and then logs in with the same credentials', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(null);
    vi.mocked(authService.register).mockResolvedValue(ANA);
    vi.mocked(authService.login).mockResolvedValue(ANA);
    renderProvider();
    await screen.findByText('nadie');

    await act(() =>
      currentAuth().register({ name: 'Ana', email: ANA.email, password: 'secret-pass' }),
    );

    expect(authService.register).toHaveBeenCalledWith({
      name: 'Ana',
      email: ANA.email,
      password: 'secret-pass',
    });
    expect(authService.login).toHaveBeenCalledWith({ email: ANA.email, password: 'secret-pass' });
    expect(await screen.findByText('Ana')).toBeInTheDocument();
  });

  test('a duplicate email stops register before the login', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(null);
    vi.mocked(authService.register).mockRejectedValue(new Error('409'));
    renderProvider();
    await screen.findByText('nadie');

    await expect(
      currentAuth().register({ name: 'Ana', email: ANA.email, password: 'secret-pass' }),
    ).rejects.toThrow('409');
    expect(authService.login).not.toHaveBeenCalled();
  });

  test('logout clears the user', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(ANA);
    vi.mocked(authService.logout).mockResolvedValue(undefined);
    renderProvider();
    await screen.findByText('Ana');

    await act(() => currentAuth().logout());

    await waitFor(() => expect(screen.getByText('nadie')).toBeInTheDocument());
    expect(authService.logout).toHaveBeenCalledOnce();
  });

  test('B3 updateUser replaces the user without asking the API again', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue(ANA);
    renderProvider();
    await screen.findByText('Ana');

    act(() => currentAuth().updateUser({ ...ANA, name: 'Ana Ruiz' }));

    expect(await screen.findByText('Ana Ruiz')).toBeInTheDocument();
    expect(authService.getCurrentUser).toHaveBeenCalledOnce();
  });
});
