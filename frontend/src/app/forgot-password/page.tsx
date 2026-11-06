'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Mail } from 'lucide-react';
import { apiClient, getApiError } from '@/lib/api-client';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setMessage('');
    setError('');
    try {
      const response = await apiClient.post<{ message: string }>('/auth/forgot-password', { email });
      setMessage(response.data.message);
    } catch (requestError) {
      setError(getApiError(requestError).message || 'We could not send the reset link. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="password-reset-page">
      <section className="password-reset-card">
        <span className="brand-mark">G</span>
        <div className="login-heading">
          <span className="form-eyebrow">ACCOUNT RECOVERY</span>
          <h2>Forgot your password?</h2>
          <p>Enter your account email and we’ll send you a secure password reset link.</p>
        </div>
        <form className="login-form" onSubmit={handleSubmit}>
          <label htmlFor="reset-email">Email address</label>
          <div className="input-with-icon"><Mail size={18} /><input id="reset-email" type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@organization.org" /></div>
          {message && <div className="password-reset-message" role="status">{message}</div>}
          {error && <div className="login-error" role="alert">{error}</div>}
          <button className="login-submit" type="submit" disabled={submitting}>{submitting ? 'Sending…' : 'Send reset link'}</button>
        </form>
        <Link className="password-reset-back" href="/login"><ArrowLeft size={14} /> Back to sign in</Link>
      </section>
    </main>
  );
}
