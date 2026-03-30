export interface User {
  id: string;
  email: string;
  created_at: string;
  email_confirmed_at?: string;
}

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'unauthenticated' | 'error';

export interface AuthError {
  code: string;
  message: string;
  type: 'network' | 'auth' | 'validation' | 'unknown';
}

export interface AuthState {
  status: AuthStatus;
  user: User | null;
  error: AuthError | null;
}

export interface AuthSession {
  access_token: string;
  refresh_token: string;
  expires_at: number;
  user: User;
}

export interface AuthCredentials {
  email: string;
  password: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordUpdateRequest {
  currentPassword: string;
  newPassword: string;
}

export type AuthEvent = 'SIGNED_IN' | 'SIGNED_OUT' | 'PASSWORD_RECOVERY' | 'TOKEN_REFRESHED';

export interface AuthEventCallback {
  (event: AuthEvent, session: AuthSession | null): void;
}
