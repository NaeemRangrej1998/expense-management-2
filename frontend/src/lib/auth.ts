import { AuthUser } from '@/types';
import { apiClient } from './api-client';

export async function fetchMe(): Promise<AuthUser> {
  const res = await apiClient.get<AuthUser>('/auth/me');
  return res.data;
}

export function hasPermission(user: AuthUser | null, permission: string): boolean {
  if (!user) return false;
  return user.permissions.includes(permission);
}

export function hasAnyPermission(user: AuthUser | null, permissions: string[]): boolean {
  if (!user) return false;
  return permissions.some((p) => user.permissions.includes(p));
}

export function isSuperAdmin(user: AuthUser | null): boolean {
  if (!user) return false;
  return user.roles.includes('Super Admin');
}

export function isAdmin(user: AuthUser | null): boolean {
  if (!user) return false;
  return user.roles.includes('Admin');
}
