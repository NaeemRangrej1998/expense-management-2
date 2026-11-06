'use client';

import Link from 'next/link';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Check,
  ChevronDown,
  CircleDollarSign,
  Clock3,
  Plus,
  RefreshCw,
  Sparkles,
  Wallet,
} from 'lucide-react';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { AppShell } from '@/components/AppShell';
import { useAuth } from '@/hooks/useAuth';
import { apiClient } from '@/lib/api-client';
import { DashboardSummary, ExpenseCategoryBreakdown, PageResponse, TransactionResponse } from '@/types';

type DashboardData = {
  summary: DashboardSummary;
  categories: ExpenseCategoryBreakdown[];
  transactions: TransactionResponse[];
};

const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

function thisMonthRange() {
  const now = new Date();
  const toDateString = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  return {
    startDate: toDateString(new Date(now.getFullYear(), now.getMonth(), 1)),
    endDate: toDateString(now),
  };
}

async function loadDashboard(permissions: string[]): Promise<DashboardData> {
  const dateRange = thisMonthRange();
  const [summary, categories, income, expenses] = await Promise.all([
    apiClient.get<DashboardSummary>('/dashboard/summary', { params: dateRange }),
    apiClient.get<ExpenseCategoryBreakdown[]>('/dashboard/expense-categories', { params: dateRange }),
    permissions.includes('income.view')
      ? apiClient.get<PageResponse<TransactionResponse>>('/transactions', { params: { type: 'INCOME', page: 0, size: 5 } })
      : Promise.resolve({ data: { content: [] as TransactionResponse[] } }),
    permissions.includes('expense.view')
      ? apiClient.get<PageResponse<TransactionResponse>>('/transactions', { params: { type: 'EXPENSE', page: 0, size: 5 } })
      : Promise.resolve({ data: { content: [] as TransactionResponse[] } }),
  ]);
  return {
    summary: summary.data,
    categories: categories.data,
    transactions: [...income.data.content, ...expenses.data.content]
      .sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime())
      .slice(0, 6),
  };
}

export default function DashboardPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const permissions = user?.permissions ?? [];
  const { data, isLoading: isDataLoading, isError, refetch } = useQuery({ queryKey: ['dashboard', permissions], queryFn: () => loadDashboard(permissions), enabled: isAuthenticated });

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthenticated, isLoading, router]);

  if (isLoading || !isAuthenticated) return <main className="page-loading"><span className="loading-mark">G</span><span>Loading your workspace…</span></main>;

  const summary = data?.summary;
  const monthLabel = summary?.startDate
    ? new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${summary.startDate}T00:00:00Z`))
    : 'This month';
  const chartData = [
    { name: 'Income', amount: Number(summary?.totalIncome ?? 0), fill: '#268e6e' },
    { name: 'Expenses', amount: Number(summary?.totalExpenses ?? 0), fill: '#dfa679' },
  ];
  const canAddIncome = user?.permissions.includes('income.create') ?? false;
  const canAddExpense = user?.permissions.includes('expense.create') ?? false;
  const addTransactionHref = canAddIncome ? '/transactions/income' : '/transactions/expense';

  return (
    <AppShell title="Overview">
      <div className="dashboard-page">
        <div className="page-heading-row">
          <div><div className="eyebrow"><span className="eyebrow-dot" /> YOUR IMPACT AT A GLANCE</div><h1>Welcome back, {user?.name?.split(' ')[0] ?? 'there'} <span className="wave">✳</span></h1><p>Here&apos;s what&apos;s happening with your organization.</p></div>
          <div className="heading-actions"><button className="button button-outline" onClick={() => void refetch()}><RefreshCw size={15} /> Refresh</button>{(canAddIncome || canAddExpense) && <Link className="button button-primary" href={addTransactionHref}><Plus size={17} /> Add transaction</Link>}</div>
        </div>
        {isError ? (
          <section className="dashboard-error"><div className="error-mark">!</div><div><h2>We couldn&apos;t load your dashboard</h2><p>Check that the backend is running and your account has dashboard access.</p><button className="text-button" onClick={() => void refetch()}>Try again <ArrowRight size={14} /></button></div></section>
        ) : (
          <>
            <div className="dashboard-toolbar"><div className="period-label"><span className="period-icon"><Clock3 size={16} /></span><strong>This month</strong><span className="period-separator">·</span><span>{monthLabel}</span><ChevronDown size={15} /></div><span className="live-status"><i /> Live data</span></div>
            <section className="stats-grid" aria-label="Financial summary">
              <article className="stat-card stat-card-balance">
                <div className="stat-top"><span className="stat-label">Available balance</span><span className="stat-icon balance-icon"><Wallet size={18} /></span></div>
                <div className="stat-value">{isDataLoading ? '—' : currency.format(Number(summary?.remainingBalance ?? 0))}</div>
                <div className="stat-foot"><span className="trend-pill">{Number(summary?.remainingBalance ?? 0) >= 0 ? <ArrowUpRight size={13} /> : <ArrowDownLeft size={13} />}{Number(summary?.remainingBalance ?? 0) >= 0 ? 'Surplus' : 'Expenses exceed income'}</span><span>Income minus expenses</span></div>
                <div className="balance-decoration"><i /><i /><i /></div>
              </article>
              <article className="stat-card">
                <div className="stat-top"><span className="stat-label">Total income</span><span className="stat-icon income-icon"><ArrowDownLeft size={18} /></span></div>
                <div className="stat-value">{isDataLoading ? '—' : currency.format(Number(summary?.totalIncome ?? 0))}</div>
                <div className="stat-foot"><span className="stat-caption">Donations and contributions</span><span className="stat-mini-bars income-bars"><i /><i /><i /><i /><i /><i /><i /></span></div>
              </article>
              <article className="stat-card">
                <div className="stat-top"><span className="stat-label">Total expenses</span><span className="stat-icon expense-icon"><ArrowUpRight size={18} /></span></div>
                <div className="stat-value">{isDataLoading ? '—' : currency.format(Number(summary?.totalExpenses ?? 0))}</div>
                <div className="stat-foot"><span className="stat-caption">Funds put to good use</span><span className="stat-mini-bars expense-bars"><i /><i /><i /><i /><i /><i /><i /></span></div>
              </article>
            </section>

            <section className="dashboard-main-grid">
              <article className="panel trend-panel">
                <div className="panel-heading"><div><h2>Income &amp; expenses</h2><p>Your money in motion this month</p></div><span className="select-button">This month <ChevronDown size={14} /></span></div>
                <div className="chart-legend"><span><i className="legend-income" /> Income</span><span><i className="legend-expense" /> Expenses</span><span className="chart-total"><CircleDollarSign size={15} /> {monthLabel}</span></div>
                <div className="chart-wrap">
                  {isDataLoading ? <div className="chart-loading">Preparing your chart…</div> : <ResponsiveContainer width="100%" height="100%"><BarChart data={chartData} layout="vertical" margin={{ top: 6, right: 8, left: -9, bottom: 0 }} barCategoryGap={24}><CartesianGrid horizontal={false} stroke="#edf0eb" strokeDasharray="3 5" /><XAxis type="number" axisLine={false} tickLine={false} tick={{ fill: '#929a91', fontSize: 10 }} tickFormatter={(value: number) => value === 0 ? '₹0' : `₹${Math.round(value / 1000)}k`} /><YAxis type="category" dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#69776c', fontSize: 11 }} width={65} /><Tooltip formatter={(value) => currency.format(Number(value ?? 0))} contentStyle={{ borderRadius: 12, border: '1px solid #e8ebe5', boxShadow: '0 8px 24px #26392b12', fontSize: 12 }} /><Bar dataKey="amount" name="Total" radius={[0, 5, 5, 0]} barSize={24}>{chartData.map((item) => <Cell key={item.name} fill={item.fill} />)}</Bar></BarChart></ResponsiveContainer>}
                </div>
                <div className="chart-footnote"><Sparkles size={14} /><span>Your organization is making a difference every day.</span></div>
              </article>
              <article className="panel category-panel">
                <div className="panel-heading"><div><h2>Where it goes</h2><p>Expense breakdown</p></div><button className="icon-button panel-more" aria-label="Expense breakdown options">···</button></div>
                <div className="category-total"><strong>{currency.format(Number(summary?.totalExpenses ?? 0))}</strong><span>total spent</span></div>
                <div className="category-list">{(data?.categories ?? []).slice(0, 5).map((category, index) => <div className="category-row" key={category.category}><span className={`category-dot category-dot-${index % 5}`} /><span className="category-name">{category.category}</span><span className="category-percent">{Math.round(category.percentage)}%</span><strong>{currency.format(Number(category.amount))}</strong></div>)}
                  {!isDataLoading && !data?.categories.length && <div className="empty-categories"><span className="empty-category-icon">◎</span><strong>No expenses yet</strong><small>Expense categories will show up here.</small></div>}
                  {isDataLoading && <div className="category-loading">Loading categories…</div>}
                </div>
                <Link className="panel-link" href="/transactions/expense">View expenses <ArrowRight size={15} /></Link>
              </article>
            </section>

            <section className="panel activity-panel">
              <div className="panel-heading activity-heading"><div><h2>Recent activity</h2><p>A little good goes a long way</p></div><div className="activity-actions"><Link className="panel-link" href="/transactions/history">View all <ArrowRight size={15} /></Link></div></div>
              <div className="activity-table-wrap"><table className="activity-table"><thead><tr><th>TRANSACTION</th><th>DATE</th><th>AREA</th><th>STATUS</th><th className="amount-heading">AMOUNT</th></tr></thead><tbody>
                {data?.transactions.map((transaction) => <tr key={transaction.id}><td><span className={`transaction-type-icon ${transaction.type === 'INCOME' ? 'type-income' : 'type-expense'}`}>{transaction.type === 'INCOME' ? <ArrowDownLeft size={16} /> : <ArrowUpRight size={16} />}</span><span className="transaction-title"><strong>{transaction.incomeCategory?.name ?? transaction.expenseCategory?.name ?? transaction.description ?? 'Transaction'}</strong><small>{transaction.transactionNumber}</small></span></td><td>{new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short' }).format(new Date(`${transaction.transactionDate}T00:00:00`))}</td><td>{transaction.area?.name ?? '—'}</td><td><span className={`status-pill status-${transaction.status.toLowerCase()}`}><i />{transaction.status.charAt(0) + transaction.status.slice(1).toLowerCase()}</span></td><td className={`amount-cell ${transaction.type === 'INCOME' ? 'amount-income' : ''}`}>{transaction.type === 'INCOME' ? '+' : '−'}{currency.format(Number(transaction.amount))}</td></tr>)}
                {!isDataLoading && !data?.transactions.length && <tr><td className="empty-activity" colSpan={5}><span className="empty-activity-mark"><Check size={18} /></span><strong>A fresh start</strong><span>New donations and expenses will appear here.</span>{(canAddIncome || canAddExpense) && <Link href={addTransactionHref}>Add your first transaction <ArrowRight size={14} /></Link>}</td></tr>}
                {isDataLoading && <tr><td className="empty-activity" colSpan={5}>Loading recent activity…</td></tr>}
              </tbody></table></div>
            </section>
            <footer className="dashboard-footer"><span>Showing live data from your organization</span><span>© Goodwell Foundation <i /> Made for doing good</span></footer>
          </>
        )}
      </div>
    </AppShell>
  );
}
