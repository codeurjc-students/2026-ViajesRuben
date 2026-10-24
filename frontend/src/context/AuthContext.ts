import { createContext } from 'react';
import type { Credentials, RegisterData, User } from '../types';

export interface AuthContextValue {
  /** The logged-in user, or null when nobody is. */
  user: User | null;
  /** True until the first answer of GET /api/v1/auth/me. */
  loading: boolean;
  login: (credentials: Credentials) => Promise<void>;
  logout: () => Promise<void>;
  /** Creates the account and logs the new user in. */
  register: (data: RegisterData) => Promise<void>;
  /** Replaces the logged-in user after the profile or the avatar changed, so the header follows. */
  updateUser: (user: User) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
