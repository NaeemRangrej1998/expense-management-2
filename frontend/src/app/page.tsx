'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

export default function Home() {
  const router = useRouter();
  const { isAuthenticated, isLoading } = useAuth();

  useEffect(() => {
    if (!isLoading) router.replace(isAuthenticated ? '/dashboard' : '/login');
  }, [isAuthenticated, isLoading, router]);

  return (
    <main className="page-loading" aria-label="Loading">
      <span className="loading-mark">G</span>
      <span>Getting your workspace ready…</span>
    </main>
  );
}
