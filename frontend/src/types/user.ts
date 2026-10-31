/** Mirrors the backend UserDTO. The avatar bytes are read from `/api/v1/images/{avatarId}`. */
export interface User {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  /** Null when the user has no avatar. */
  avatarId: string | null;
  /** ISO date (`2026-10-24`). */
  registrationDate: string;
}

export type UserStatus = 'ACTIVE' | 'BLOCKED';

/** Mirrors the backend AdminUserDTO: a user as the administrator manages them. */
export interface AdminUser {
  id: number;
  name: string;
  email: string;
  status: UserStatus;
  /** ISO date (`2026-10-24`). */
  registrationDate: string;
}

export interface Credentials {
  email: string;
  password: string;
}

export interface RegisterData extends Credentials {
  name: string;
}

/** The editable details of one's own profile. */
export interface ProfileData {
  name: string;
  email: string;
}

export interface PasswordChange {
  currentPassword: string;
  newPassword: string;
}
