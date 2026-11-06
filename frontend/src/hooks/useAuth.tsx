'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { AuthUser } from '@/types';
import { apiClient } from '@/lib/api-client';
import { hasPermission, isSuperAdmin } from '@/lib/auth';
import { toast } from 'sonner';

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  can: (permission: string) => boolean;
  isSuperAdmin: () => boolean;
  setUser: (user: AuthUser | null) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<AuthUser>('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false));
  }, []);

  const can = (permission: string) => hasPermission(user, permission);
  const superAdmin = () => isSuperAdmin(user);

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      toast.error('The server could not confirm sign out. Your local session was cleared.');
    }
    setUser(null);
    router.push('/login');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        can,
        isSuperAdmin: superAdmin,
        setUser,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
