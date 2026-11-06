'use client';

import { FormEvent, Suspense, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, LockKeyhole } from 'lucide-react';
import { useSearchParams } from 'next/navigation';
import { apiClient, getApiError } from '@/lib/api-client';

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage('');
    setError('');
    if (!token) {
      setError('This reset link is missing its token. Request a new reset link.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await apiClient.post<{ message: string }>('/auth/reset-password', {
        token,
        newPassword: password,
        confirmPassword,
      });
      setMessage(response.data.message);
    } catch (requestError) {
      setError(getApiError(requestError).message || 'We could not reset your password. Request a new link and try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="password-reset-page">
      <section className="password-reset-card">
        <span className="brand-mark">G</span>
        <div className="login-heading">
          <span className="form-eyebrow">SECURE YOUR ACCOUNT</span>
          <h2>Set a new password</h2>
          <p>Choose a password with at least 8 characters.</p>
        </div>
        <form className="login-form" onSubmit={handleSubmit}>
          <label htmlFor="new-password">New password</label>
          <div className="input-with-icon"><LockKeyhole size={18} /><input id="new-password" type="password" autoComplete="new-password" minLength={8} required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" /></div>
          <label htmlFor="confirm-password">Confirm password</label>
          <div className="input-with-icon"><LockKeyhole size={18} /><input id="confirm-password" type="password" autoComplete="new-password" minLength={8} required value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} placeholder="Enter it again" /></div>
          {message && <div className="password-reset-message" role="status">{message}</div>}
          {error && <div className="login-error" role="alert">{error}</div>}
          {!message && <button className="login-submit" type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Set new password'}</button>}
        </form>
        <Link className="password-reset-back" href="/login"><ArrowLeft size={14} /> Back to sign in</Link>
      </section>
    </main>
  );
}

export default function ResetPasswordPage() {
  return <Suspense fallback={<main className="password-reset-page"><div className="password-reset-card">Loading password reset…</div></main>}><ResetPasswordForm /></Suspense>;
}
