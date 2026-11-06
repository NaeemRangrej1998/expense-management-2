'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowRight, Eye, EyeOff, HeartHandshake, LockKeyhole, Mail } from 'lucide-react';
import { apiClient, getApiError } from '@/lib/api-client';
import { useAuth } from '@/hooks/useAuth';
import { AuthUser } from '@/types';

export default function LoginPage() {
  const router = useRouter();
  const { setUser, isAuthenticated, isLoading } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isLoading && isAuthenticated) router.replace('/dashboard');
  }, [isAuthenticated, isLoading, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const response = await apiClient.post<AuthUser>('/auth/login', { email, password });
      setUser(response.data);
      router.replace('/dashboard');
    } catch (requestError) {
      setError(getApiError(requestError).message || 'We could not sign you in. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-story">
        <div className="login-story-top">
          <span className="brand-mark brand-mark-light">G</span>
          <span className="login-brand-name">goodwell</span>
        </div>
        <div className="story-content">
          <div className="story-eyebrow"><span /> MAKING GOOD, GO FURTHER</div>
          <h1>Every contribution<br />has a <em>story.</em></h1>
          <p>One calm, clear place to care for every donation, expense, and the people behind them.</p>
          <div className="story-art" aria-hidden="true">
            <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
            <div className="art-card art-card-main"><span className="art-card-icon"><HeartHandshake size={20} /></span><span className="art-label">COMMUNITY FUND</span><strong>Small acts,<br />lasting change.</strong><span className="art-amount">Giving with purpose <small>every contribution matters</small></span><div className="art-progress"><i /></div><div className="art-avatars"><b>AS</b><b>MK</b><b>+</b><small>making an impact together</small></div></div>
            <div className="art-note"><span className="note-check">✓</span><span><strong>Every rupee accounted for</strong><small>Transparent by design</small></span></div>
            <span className="art-sparkle sparkle-one">✳</span><span className="art-sparkle sparkle-two">✳</span>
          </div>
        </div>
        <div className="login-story-footer"><span>© 2026 Goodwell Foundation</span><span>Built for the good you do.</span></div>
      </section>
      <section className="login-form-side">
        <div className="login-form-wrap">
          <div className="login-mobile-brand"><span className="brand-mark">G</span><strong>goodwell</strong></div>
          <div className="login-heading"><span className="form-eyebrow">WELCOME BACK</span><h2>Sign in to your account</h2><p>Pick up where your good work left off.</p></div>
          <form className="login-form" onSubmit={handleSubmit}>
            <label htmlFor="email">Email address</label>
            <div className="input-with-icon"><Mail size={18} /><input id="email" type="email" placeholder="you@organization.org" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></div>
            <div className="password-label"><label htmlFor="password">Password</label><a href="/forgot-password">Forgot password?</a></div>
            <div className="input-with-icon"><LockKeyhole size={18} /><input id="password" type={showPassword ? 'text' : 'password'} placeholder="Enter your password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /><button type="button" className="password-toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div>
            {error && <div className="login-error" role="alert">{error}</div>}
            <button className="login-submit" type="submit" disabled={submitting}>{submitting ? 'Signing you in…' : 'Sign in'}{!submitting && <ArrowRight size={17} />}</button>
          </form>
          <div className="login-security"><LockKeyhole size={14} /><span>Your information is encrypted and secure</span></div>
        </div>
        <div className="login-side-footer"><span>Need access? Contact your organization admin.</span><a href="mailto:support@goodwell.org">Get help</a></div>
      </section>
    </main>
  );
}
