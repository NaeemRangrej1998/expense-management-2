'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Plus,
  Search,
  SlidersHorizontal,
  XCircle,
  X,
} from 'lucide-react';
import { AppShell } from '@/components/AppShell';
import { useAuth } from '@/hooks/useAuth';
import { apiClient, getApiError } from '@/lib/api-client';
import { PageResponse, TransactionResponse, TransactionType } from '@/types';
import { toast } from 'sonner';

type CategoryOption = { id: number; name: string };
type AreaOption = { id: number; name: string };
type TransactionForm = {
  amount: string;
  categoryId: string;
  areaId: string;
  transactionDate: string;
  paymentMethod: string;
  referenceNumber: string;
  description: string;
  billFile: File | null;
};

const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const paymentMethods = ['CASH', 'BANK_TRANSFER', 'CHEQUE', 'UPI', 'CARD', 'OTHER'];

function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export default function TransactionsView({ type, initialSearch }: { type: string; initialSearch: string }) {
  const router = useRouter();
  const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();
  const queryClient = useQueryClient();
  const routeType = type.toLowerCase();
  const isHistory = routeType === 'history';
  const transactionType: TransactionType | undefined = routeType === 'income' ? 'INCOME' : routeType === 'expense' ? 'EXPENSE' : undefined;
  const [searchText, setSearchText] = useState(initialSearch);
  const [search, setSearch] = useState(initialSearch);
  const [statusFilter, setStatusFilter] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState('');
  const [rejectTarget, setRejectTarget] = useState<TransactionResponse | null>(null);
  const [rejectionComments, setRejectionComments] = useState('');
  const [form, setForm] = useState<TransactionForm>({
    amount: '',
    categoryId: '',
    areaId: '',
    transactionDate: '',
    paymentMethod: 'CASH',
    referenceNumber: '',
    description: '',
    billFile: null,
  });
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthLoading, isAuthenticated, router]);

  const title = isHistory ? 'All transactions' : transactionType === 'INCOME' ? 'Income' : 'Expenses';
  const canCreate = user?.permissions.includes(transactionType === 'INCOME' ? 'income.create' : 'expense.create') ?? false;
  const isSuperAdmin = user?.roles.includes('Super Admin') ?? false;
  const canReviewAny = isSuperAdmin || (user?.permissions.includes('income.approve') ?? false) || (user?.permissions.includes('income.reject') ?? false);
  const listQuery = useQuery({
    queryKey: ['transactions', routeType, search, statusFilter, page],
    enabled: isAuthenticated && (isHistory || !!transactionType),
    queryFn: async () => {
      const endpoint = isHistory ? '/transactions/history' : '/transactions';
      const response = await apiClient.get<PageResponse<TransactionResponse>>(endpoint, {
        params: {
          ...(transactionType && { type: transactionType }),
          ...(search && { search }),
          ...(!isHistory && statusFilter && { status: statusFilter }),
          page,
          size: 10,
        },
      });
      return response.data;
    },
  });

  const categoryQuery = useQuery({
    queryKey: ['transaction-categories', transactionType],
    enabled: isAuthenticated && !!transactionType && showForm,
    queryFn: async () => {
      const endpoint = transactionType === 'INCOME' ? '/categories/income' : '/categories/expense';
      const response = await apiClient.get<CategoryOption[]>(endpoint);
      return response.data;
    },
  });
  const areaQuery = useQuery({
    queryKey: ['transaction-areas'],
    enabled: isAuthenticated && showForm,
    queryFn: async () => (await apiClient.get<AreaOption[]>('/areas/active')).data,
  });

  const createMutation = useMutation({
    mutationFn: async ({ payload, file }: { payload: Record<string, unknown>; file: File }) => {
      const body = new FormData();
      body.append('transaction', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
      body.append('file', file);
      return apiClient.post('/transactions', body, { headers: { 'Content-Type': 'multipart/form-data' } });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['transactions'] });
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setShowForm(false);
      setFormError('');
      setForm({ amount: '', categoryId: '', areaId: '', transactionDate: localDateString(new Date()), paymentMethod: 'CASH', referenceNumber: '', description: '', billFile: null });
    },
    onError: (error) => setFormError(getApiError(error).message || 'Could not save this transaction. Please try again.'),
  });

  const reviewMutation = useMutation({
    mutationFn: async ({ transaction, action, comments }: { transaction: TransactionResponse; action: 'approve' | 'reject'; comments?: string }) =>
      apiClient.post(`/transactions/${transaction.id}/${action}`, { ...(comments?.trim() && { comments: comments.trim() }) }),
    onSuccess: async (_response, variables) => {
      await queryClient.invalidateQueries({ queryKey: ['transactions'] });
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success(`Transaction ${variables.action === 'approve' ? 'approved' : 'rejected'}.`);
      setRejectTarget(null);
      setRejectionComments('');
    },
    onError: (error) => toast.error(getApiError(error).message || 'Could not update transaction status.'),
  });

  const rows = useMemo(() => listQuery.data?.content ?? [], [listQuery.data?.content]);
  const pageCount = listQuery.data?.totalPages ?? 0;
  const amountTotals = useMemo(() => rows.reduce(
    (totals, transaction) => {
      if (transaction.type === 'INCOME') totals.income += Number(transaction.amount);
      else totals.expenses += Number(transaction.amount);
      return totals;
    },
    { income: 0, expenses: 0 },
  ), [rows]);

  if (isAuthLoading || !isAuthenticated) return <main className="page-loading"><span className="loading-mark">G</span><span>Loading your workspace…</span></main>;
  if (!isHistory && !transactionType) return <AppShell title="Not found"><div className="dashboard-error"><div><h2>Page not found</h2><p>Choose an item from the workspace menu.</p></div></div></AppShell>;

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setSearch(searchText.trim());
  }

  function submitTransaction(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError('');
    if (!transactionType) return;
    if (!form.billFile) {
      setFormError('Attach the transaction bill or receipt before saving.');
      return;
    }
    const extension = form.billFile.name.split('.').pop()?.toLowerCase();
    if (!extension || !['pdf', 'png', 'jpg', 'jpeg'].includes(extension)) {
      setFormError('Choose a PDF, PNG, JPG, or JPEG bill.');
      return;
    }
    if (form.billFile.size > 5 * 1024 * 1024) {
      setFormError('The bill must be 5 MB or smaller.');
      return;
    }
    createMutation.mutate({
      file: form.billFile,
      payload: {
        type: transactionType,
        amount: Number(form.amount),
        ...(transactionType === 'INCOME' ? { incomeCategoryId: Number(form.categoryId) } : { expenseCategoryId: Number(form.categoryId) }),
        ...(form.areaId && { areaId: Number(form.areaId) }),
        transactionDate: form.transactionDate,
        paymentMethod: form.paymentMethod,
        ...(form.referenceNumber && { referenceNumber: form.referenceNumber }),
        ...(form.description && { description: form.description }),
      },
    });
  }

  function canApprove(transaction: TransactionResponse) {
    return transaction.type === 'INCOME'
      ? isSuperAdmin || (user?.permissions.includes('income.approve') ?? false)
      : isSuperAdmin;
  }

  function canReject(transaction: TransactionResponse) {
    return transaction.type === 'INCOME'
      ? isSuperAdmin || (user?.permissions.includes('income.reject') ?? false)
      : isSuperAdmin;
  }

  function submitRejection(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!rejectTarget) return;
    reviewMutation.mutate({ transaction: rejectTarget, action: 'reject', comments: rejectionComments });
  }

  const tableColumnCount = canReviewAny ? 7 : 6;

  return (
    <AppShell title={title}>
      <div className="transactions-page">
        <div className="page-heading-row">
          <div><div className="eyebrow"><span className="eyebrow-dot" /> FINANCIAL ACTIVITY</div><h1>{title}</h1><p>{isHistory ? 'A complete record of your organization’s financial activity.' : transactionType === 'INCOME' ? 'Every contribution helps your mission move forward.' : 'Track the resources you put to meaningful work.'}</p></div>
          {canCreate && <button className="button button-primary" onClick={() => { setForm((current) => ({ ...current, transactionDate: current.transactionDate || localDateString(new Date()), billFile: null })); setShowForm(true); }}><Plus size={17} /> Add {transactionType === 'INCOME' ? 'income' : 'expense'}</button>}
        </div>

        <div className="transaction-summary-strip">
          <div><span className="summary-icon summary-count"><SlidersHorizontal size={17} /></span><span><small>ON THIS PAGE</small><strong>{listQuery.data?.totalElements ?? '—'} <em>transactions</em></strong></span></div>
          {(isHistory || transactionType === 'INCOME') && <div><span className="summary-icon summary-income"><ArrowDownLeft size={17} /></span><span><small>INCOME SHOWN</small><strong>{listQuery.isLoading ? '—' : currency.format(amountTotals.income)}</strong></span></div>}
          {(isHistory || transactionType === 'EXPENSE') && <div><span className="summary-icon summary-expense"><ArrowUpRight size={17} /></span><span><small>EXPENSES SHOWN</small><strong>{listQuery.isLoading ? '—' : currency.format(amountTotals.expenses)}</strong></span></div>}
        </div>

        <section className="panel transactions-panel">
          <div className="transaction-toolbar">
            <form className="transaction-search" onSubmit={submitSearch}><Search size={16} /><input placeholder="Search reference, description…" value={searchText} onChange={(event) => setSearchText(event.target.value)} /><button type="submit">Search</button></form>
            <div className="transaction-filters">{!isHistory && <label className="filter-select"><SlidersHorizontal size={15} /><select aria-label="Filter by status" value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0); }}><option value="">All statuses</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select></label>}<span className="date-filter-static"><CalendarDays size={15} /> All dates</span></div>
          </div>
          <div className="activity-table-wrap"><table className="activity-table transactions-table"><thead><tr><th>TRANSACTION</th><th>DATE</th><th>AREA</th><th>ADDED BY</th><th>STATUS</th><th className="amount-heading">AMOUNT</th>{canReviewAny && <th className="transaction-actions-heading">REVIEW</th>}</tr></thead><tbody>
            {rows.map((transaction) => <tr key={transaction.id}><td><span className={`transaction-type-icon ${transaction.type === 'INCOME' ? 'type-income' : 'type-expense'}`}>{transaction.type === 'INCOME' ? <ArrowDownLeft size={16} /> : <ArrowUpRight size={16} />}</span><span className="transaction-title"><strong>{transaction.incomeCategory?.name ?? transaction.expenseCategory?.name ?? transaction.description ?? 'Transaction'}</strong><small>{transaction.transactionNumber}{transaction.referenceNumber ? ` · ${transaction.referenceNumber}` : ''}</small></span></td><td>{new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(`${transaction.transactionDate}T00:00:00`))}</td><td>{transaction.area?.name ?? '—'}</td><td>{transaction.createdBy?.name ?? '—'}</td><td><span className={`status-pill status-${transaction.status.toLowerCase()}`}><i />{transaction.status.charAt(0) + transaction.status.slice(1).toLowerCase()}</span></td><td className={`amount-cell ${transaction.type === 'INCOME' ? 'amount-income' : ''}`}>{transaction.type === 'INCOME' ? '+' : '−'}{currency.format(Number(transaction.amount))}</td>{canReviewAny && <td className="transaction-review-cell">{transaction.status === 'PENDING' && <span className="transaction-review-actions">{canApprove(transaction) && <button type="button" className="review-approve-button" disabled={reviewMutation.isPending} onClick={() => reviewMutation.mutate({ transaction, action: 'approve' })}><CheckCircle2 size={14} /> Approve</button>}{canReject(transaction) && <button type="button" className="review-reject-button" disabled={reviewMutation.isPending} onClick={() => { setRejectTarget(transaction); setRejectionComments(''); }}><XCircle size={14} /> Reject</button>}</span>}</td>}</tr>)}
            {listQuery.isLoading && <tr><td className="table-message" colSpan={tableColumnCount}>Loading transactions…</td></tr>}
            {listQuery.isError && <tr><td className="table-message table-error" colSpan={tableColumnCount}>{getApiError(listQuery.error).message} <button type="button" onClick={() => void listQuery.refetch()}>Try again</button></td></tr>}
            {!listQuery.isLoading && !listQuery.isError && rows.length === 0 && <tr><td className="empty-activity" colSpan={tableColumnCount}><span className="empty-activity-mark"><Check size={18} /></span><strong>Nothing to see here yet</strong><span>When you add a transaction, it will appear here.</span>{canCreate && <button className="inline-action" onClick={() => setShowForm(true)}>Add a transaction <ArrowRight size={14} /></button>}</td></tr>}
          </tbody></table></div>
          <div className="table-pagination"><span>{listQuery.data ? `Showing ${rows.length ? page * 10 + 1 : 0}–${page * 10 + rows.length} of ${listQuery.data.totalElements}` : 'Loading results'}</span><div><button className="pagination-button" disabled={page === 0 || listQuery.isLoading} onClick={() => setPage((value) => Math.max(0, value - 1))} aria-label="Previous page"><ChevronLeft size={16} /></button><span>Page {page + 1} of {Math.max(pageCount, 1)}</span><button className="pagination-button" disabled={page + 1 >= pageCount || listQuery.isLoading} onClick={() => setPage((value) => value + 1)} aria-label="Next page"><ChevronRight size={16} /></button></div></div>
        </section>
      </div>

      {showForm && transactionType && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setShowForm(false); }}><section className="transaction-modal" role="dialog" aria-modal="true" aria-labelledby="transaction-modal-title"><div className="modal-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> NEW {transactionType}</div><h2 id="transaction-modal-title">Add {transactionType.toLowerCase()}</h2><p>Keep a clear record of every rupee.</p></div><button className="icon-button" onClick={() => setShowForm(false)} aria-label="Close dialog"><X size={19} /></button></div>
        <form className="transaction-form" onSubmit={submitTransaction}>
          <label className="form-field form-field-wide"><span>Amount <b>*</b></span><div className="amount-input"><span>₹</span><input type="number" min="0.01" step="0.01" placeholder="0.00" required value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} /></div></label>
          <label className="form-field form-field-wide"><span>Category <b>*</b></span><select required value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">{categoryQuery.isLoading ? 'Loading categories…' : 'Choose a category'}</option>{categoryQuery.data?.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}</select>{categoryQuery.isError && <small className="field-hint field-error">Could not load categories. Check your permissions.</small>}</label>
          <label className="form-field"><span>Date <b>*</b></span><input type="date" required value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} /></label>
          <label className="form-field"><span>Payment method <b>*</b></span><select required value={form.paymentMethod} onChange={(event) => setForm({ ...form, paymentMethod: event.target.value })}>{paymentMethods.map((method) => <option key={method} value={method}>{method.replace('_', ' ')}</option>)}</select></label>
          <label className="form-field form-field-wide"><span>Area <small>Optional</small></span><select value={form.areaId} onChange={(event) => setForm({ ...form, areaId: event.target.value })}><option value="">{areaQuery.isLoading ? 'Loading areas…' : 'No area selected'}</option>{areaQuery.data?.map((area) => <option value={area.id} key={area.id}>{area.name}</option>)}</select>{areaQuery.isError && <small className="field-hint field-error">Could not load areas. Check your permissions.</small>}</label>
          <label className="form-field form-field-wide"><span>Reference number <small>Optional</small></span><input maxLength={200} placeholder="e.g. receipt or transfer ID" value={form.referenceNumber} onChange={(event) => setForm({ ...form, referenceNumber: event.target.value })} /></label>
          <label className="form-field form-field-wide"><span>Note <small>Optional</small></span><textarea maxLength={2000} rows={3} placeholder="Add a short description…" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
          <label className="form-field form-field-wide"><span>Bill / receipt <b>*</b></span><input className="bill-file-input" type="file" accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg" required onChange={(event) => setForm({ ...form, billFile: event.target.files?.[0] ?? null })} /><small className="field-hint">Required. PDF, PNG, JPG, or JPEG; max 5 MB.</small>{form.billFile && <small className="bill-file-selected">{form.billFile.name}</small>}</label>
          {formError && <div className="login-error form-error" role="alert">{formError}</div>}
          <div className="modal-actions"><button type="button" className="button button-outline" onClick={() => setShowForm(false)}>Cancel</button><button type="submit" className="button button-primary" disabled={createMutation.isPending || categoryQuery.isLoading}>{createMutation.isPending ? 'Saving…' : 'Save transaction'}{!createMutation.isPending && <ArrowRight size={16} />}</button></div>
        </form>
      </section></div>}
      {rejectTarget && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !reviewMutation.isPending) setRejectTarget(null); }}><section className="transaction-modal review-modal" role="dialog" aria-modal="true" aria-labelledby="reject-modal-title"><div className="modal-heading"><div><div className="eyebrow"><span className="eyebrow-dot" /> TRANSACTION REVIEW</div><h2 id="reject-modal-title">Reject transaction?</h2><p>{rejectTarget.transactionNumber} · {currency.format(Number(rejectTarget.amount))}</p></div><button className="icon-button" disabled={reviewMutation.isPending} onClick={() => setRejectTarget(null)} aria-label="Close dialog"><X size={19} /></button></div><form className="transaction-form review-form" onSubmit={submitRejection}><label className="form-field form-field-wide"><span>Reason <small>Optional</small></span><textarea rows={3} maxLength={500} value={rejectionComments} onChange={(event) => setRejectionComments(event.target.value)} placeholder="Add a note about this decision." /></label><div className="modal-actions"><button type="button" className="button button-outline" disabled={reviewMutation.isPending} onClick={() => setRejectTarget(null)}>Cancel</button><button type="submit" className="button review-confirm-reject" disabled={reviewMutation.isPending}>{reviewMutation.isPending ? 'Rejecting…' : 'Reject transaction'}<XCircle size={15} /></button></div></form></section></div>}
    </AppShell>
  );
}
