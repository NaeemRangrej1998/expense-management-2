'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { CalendarDays, Download, FileText } from 'lucide-react';
import axios from 'axios';
import { AppShell } from '@/components/AppShell';
import { useAuth } from '@/hooks/useAuth';
import { apiClient, getApiError } from '@/lib/api-client';
import { ApiError } from '@/types';

function todayString() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export default function ReportsPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace('/login');
  }, [isLoading, isAuthenticated, router]);

  if (isLoading || !isAuthenticated) {
    return <main className="page-loading"><span className="loading-mark">G</span><span>Loading your workspace…</span></main>;
  }

  const canView = user?.permissions.includes('reports.view') ?? false;

  async function exportReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    if (startDate && endDate && startDate > endDate) {
      setError('Start date must be on or before the end date.');
      return;
    }

    setDownloading(true);
    try {
      const response = await apiClient.get<Blob>('/reports/transactions/export', {
        params: {
          ...(type && { type }),
          ...(status && { status }),
          ...(startDate && { startDate }),
          ...(endDate && { endDate }),
        },
        responseType: 'blob',
      });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `transactions_${todayString()}.csv`;
      document.body.append(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (requestError) {
      if (axios.isAxiosError(requestError) && requestError.response?.data instanceof Blob) {
        try {
          const parsed = JSON.parse(await requestError.response.data.text()) as ApiError;
          setError(parsed.message || 'Could not export the report.');
        } catch {
          setError(getApiError(requestError).message || 'Could not export the report.');
        }
      } else {
        setError(getApiError(requestError).message || 'Could not export the report.');
      }
    } finally {
      setDownloading(false);
    }
  }

  return (
    <AppShell title="Reports">
      <div className="reports-page">
        <div className="page-heading-row">
          <div><div className="eyebrow"><span className="eyebrow-dot" /> WORKSPACE INSIGHTS</div><h1>Reports</h1><p>Export transaction records for your organization.</p></div>
        </div>
        {!canView ? (
          <section className="panel admin-empty"><div className="admin-empty-icon"><FileText size={19} /></div><h2>You don&apos;t have access to reports</h2><p>Ask an organization administrator to grant the <code>reports.view</code> permission.</p></section>
        ) : (
          <section className="panel report-export-card">
            <div className="report-card-heading"><span className="report-icon"><FileText size={19} /></span><div><h2>Transaction report</h2><p>Download a CSV file with optional transaction and date filters.</p></div></div>
            <form className="report-filter-form" onSubmit={exportReport}>
              <label className="form-field"><span>Transaction type</span><select value={type} onChange={(event) => setType(event.target.value)}><option value="">All types</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option></select></label>
              <label className="form-field"><span>Status</span><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All statuses</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option><option value="CANCELLED">Cancelled</option></select></label>
              <label className="form-field"><span>Start date</span><span className="report-date-input"><CalendarDays size={15} /><input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} /></span></label>
              <label className="form-field"><span>End date</span><span className="report-date-input"><CalendarDays size={15} /><input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} /></span></label>
              {error && <div className="login-error form-error report-error" role="alert">{error}</div>}
              <div className="report-form-actions"><span>Export format: CSV</span><button className="button button-primary" type="submit" disabled={downloading}>{downloading ? 'Preparing report…' : 'Download report'}<Download size={15} /></button></div>
            </form>
          </section>
        )}
      </div>
    </AppShell>
  );
}
