import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { AuthContext } from '../context/AuthContext';
import * as authService from '../services/authService';
import type { Credentials, RegisterData, User } from '../types';

/** Asks the API who is logged in on load and keeps that answer for the whole app. */
export default function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    authService
      .getCurrentUser()
      .then((current) => {
        if (!cancelled) setUser(current);
      })
      // A failed check (server down) is treated as logged out; the pages show their own errors.
      .catch(() => {
        if (!cancelled) setUser(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (credentials: Credentials) => {
    setUser(await authService.login(credentials));
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setUser(null);
  }, []);

  const register = useCallback(
    async (data: RegisterData) => {
      await authService.register(data);
      await login({ email: data.email, password: data.password });
    },
    [login],
  );

  const value = useMemo(
    () => ({ user, loading, login, logout, register }),
    [user, loading, login, logout, register],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
