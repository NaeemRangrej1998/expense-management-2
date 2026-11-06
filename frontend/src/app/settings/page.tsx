'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, KeyRound, MapPin, ShieldCheck, UserRound } from 'lucide-react';
import { AppShell } from '@/components/AppShell';
import { useAuth } from '@/hooks/useAuth';

export default function SettingsPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthenticated, isLoading, router]);

  if (isLoading || !isAuthenticated) return <main className="page-loading"><span className="loading-mark">G</span><span>Loading your workspace…</span></main>;

  return (
    <AppShell title="Account settings">
      <div className="settings-page">
        <div className="page-heading-row">
          <div><div className="eyebrow"><span className="eyebrow-dot" /> YOUR PROFILE</div><h1>Account settings</h1><p>Your account details and access at a glance.</p></div>
          <Link className="button button-outline" href="/dashboard"><ArrowLeft size={15} /> Back to overview</Link>
        </div>
        <section className="panel settings-profile">
          <div className="settings-profile-avatar">{user?.name?.slice(0, 1).toUpperCase() ?? 'U'}</div>
          <div className="settings-profile-copy"><h2>{user?.name}</h2><p>{user?.email}</p></div>
          <span className="settings-member-pill"><i /> Active account</span>
        </section>
        <div className="settings-grid">
          <section className="panel settings-card">
            <div className="settings-card-heading"><span className="settings-card-icon"><UserRound size={17} /></span><div><h2>Personal information</h2><p>Identity details for your account.</p></div></div>
            <dl className="settings-details"><div><dt>Full name</dt><dd>{user?.name || '—'}</dd></div><div><dt>Email address</dt><dd>{user?.email || '—'}</dd></div><div><dt>Account ID</dt><dd>GW-{user?.userId ?? '—'}</dd></div></dl>
          </section>
          <section className="panel settings-card">
            <div className="settings-card-heading"><span className="settings-card-icon settings-shield"><ShieldCheck size={17} /></span><div><h2>Role &amp; access</h2><p>Access is managed by your organization.</p></div></div>
            <div className="settings-chip-list">{(user?.roles ?? []).map((role) => <span className="settings-role-chip" key={role}><ShieldCheck size={13} />{role}</span>)}</div>
            <div className="settings-access-foot"><KeyRound size={14} /><span>{user?.permissions.length ?? 0} permissions assigned</span></div>
          </section>
          <section className="panel settings-card settings-areas-card">
            <div className="settings-card-heading"><span className="settings-card-icon settings-area-icon"><MapPin size={17} /></span><div><h2>Assigned areas</h2><p>Areas included in your workspace access.</p></div></div>
            <div className="settings-chip-list">{user?.areas.length ? user.areas.map((area) => <span className="settings-area-chip" key={area.id}><MapPin size={12} />{area.name}</span>) : <span className="settings-no-areas">All organization areas</span>}</div>
          </section>
        </div>
        <div className="settings-note"><ShieldCheck size={16} /><span>Need to update your details or access? Please contact your organization administrator.</span></div>
      </div>
    </AppShell>
  );
}
