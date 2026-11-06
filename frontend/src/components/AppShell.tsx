'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  ArrowUpRight as ExternalArrow,
  ChevronDown,
  FileText,
  KeyRound,
  LayoutDashboard,
  LogOut,
  MapPin,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  ShieldCheck,
  Settings,
  Users,
  X,
} from 'lucide-react';
import { FormEvent, ReactNode, useState } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { useRouter } from 'next/navigation';

const links = [
  { href: '/dashboard', label: 'Overview', icon: LayoutDashboard, permission: 'dashboard.view' },
  { href: '/reports', label: 'Reports', icon: FileText, permission: 'reports.view' },
];

const transactionLinks = [
  { href: '/transactions/income', label: 'Income', icon: ArrowDownLeft, permission: 'income.view' },
  { href: '/transactions/expense', label: 'Expenses', icon: ArrowUpRight, permission: 'expense.view' },
  { href: '/transactions/history', label: 'All transactions', icon: ArrowLeftRight, permission: 'transaction_history.view' },
];

const settingsLinks = [
  { href: '/settings', label: 'Account settings', icon: Settings },
  { href: '/settings/users', label: 'Users', icon: Users, permission: 'users.view' },
  { href: '/settings/roles', label: 'Roles', icon: ShieldCheck, permission: 'roles.view' },
  { href: '/settings/permissions', label: 'Permissions', icon: KeyRound, permission: 'permissions.view' },
  { href: '/settings/areas', label: 'Areas', icon: MapPin, permission: 'areas.view' },
];

export function AppShell({ children, title }: { children: ReactNode; title: string }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [globalSearch, setGlobalSearch] = useState('');
  const [transactionsOpen, setTransactionsOpen] = useState(pathname.startsWith('/transactions'));
  const [settingsOpen, setSettingsOpen] = useState(true);
  const visibleTransactionLinks = transactionLinks.filter(({ permission }) => user?.permissions.includes(permission));
  const visibleSettingsLinks = settingsLinks.filter(({ permission }) =>
    !permission || user?.permissions.includes(permission),
  );

  function submitGlobalSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const query = globalSearch.trim();
    router.push(`/transactions/history${query ? `?search=${encodeURIComponent(query)}` : ''}`);
  }

  return (
    <div className={`app-shell ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      {sidebarOpen && <button className="sidebar-backdrop" onClick={() => setSidebarOpen(false)} aria-label="Close menu" />}
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand-row">
          <Link className="brand" href="/dashboard" onClick={() => setSidebarOpen(false)} title="Goodwell home">
            <span className="brand-mark">G</span>
            <span className="brand-copy"><strong>goodwell</strong><small>DONATION MANAGER</small></span>
          </Link>
          <button className="icon-button sidebar-collapse-toggle" onClick={() => setSidebarCollapsed((collapsed) => !collapsed)} aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'} title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
            {sidebarCollapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          </button>
        </div>
        <button className="sidebar-close icon-button" onClick={() => setSidebarOpen(false)} aria-label="Close menu"><X size={18} /></button>

        <div className="workspace-switcher">
          <span className="workspace-avatar">G</span>
          <span className="workspace-copy"><strong>Donation workspace</strong><small>Organization account</small></span>
          <ChevronDown size={15} />
        </div>

        <div className="nav-heading">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          {links.filter(({ permission }) => user?.permissions.includes(permission)).map(({ href, label, icon: Icon }) => {
            const active = pathname === href || (href !== '/dashboard' && pathname.startsWith(`${href}/`));
            return (
              <Link className={`nav-link ${active ? 'nav-link-active' : ''}`} href={href} key={href} title={label} onClick={() => setSidebarOpen(false)}>
                <Icon size={18} strokeWidth={1.8} /><span>{label}</span>
              </Link>
            );
          })}
        </nav>
        {visibleTransactionLinks.length > 0 && <div className="settings-nav">
          <button className={`nav-link settings-nav-trigger ${pathname.startsWith('/transactions') ? 'nav-link-active' : ''}`} onClick={() => setTransactionsOpen((open) => !open)} aria-expanded={transactionsOpen} aria-controls="transactions-nav-links" title="Transactions">
            <ArrowLeftRight size={18} strokeWidth={1.8} /><span>Transactions</span><ChevronDown className={`settings-chevron ${transactionsOpen ? 'settings-chevron-open' : ''}`} size={14} />
          </button>
          {transactionsOpen && <nav className="side-nav settings-subnav" id="transactions-nav-links" aria-label="Transactions">
            {visibleTransactionLinks.map(({ href, label, icon: TransactionIcon }) => {
              const active = pathname === href || pathname.startsWith(`${href}/`);
              return <Link className={`nav-link settings-subnav-link ${active ? 'nav-link-active' : ''}`} href={href} key={href} title={label} onClick={() => setSidebarOpen(false)}><TransactionIcon size={16} strokeWidth={1.8} /><span>{label}</span>{label === 'Income' && <span className="nav-indicator income-indicator" />}{label === 'Expenses' && <span className="nav-indicator expense-indicator" />}</Link>;
            })}
          </nav>}
        </div>}
        {visibleSettingsLinks.length > 0 && <div className="settings-nav">
          <button className={`nav-link settings-nav-trigger ${pathname.startsWith('/settings') ? 'nav-link-active' : ''}`} onClick={() => setSettingsOpen((open) => !open)} aria-expanded={settingsOpen} aria-controls="settings-nav-links" title="Settings">
            <Settings size={18} strokeWidth={1.8} /><span>Settings</span><ChevronDown className={`settings-chevron ${settingsOpen ? 'settings-chevron-open' : ''}`} size={14} />
          </button>
          {settingsOpen && <nav className="side-nav settings-subnav" id="settings-nav-links" aria-label="Settings">
            {visibleSettingsLinks.map(({ href, label, icon: SettingIcon }) => {
              const active = pathname === href;
              return <Link className={`nav-link settings-subnav-link ${active ? 'nav-link-active' : ''}`} href={href} key={href} title={label} onClick={() => setSidebarOpen(false)}><SettingIcon size={16} strokeWidth={1.8} /><span>{label}</span></Link>;
            })}
          </nav>}
        </div>}

        <div className="sidebar-spacer" />
        <div className="sidebar-bottom">
          <div className="profile-card">
            <div className="profile-avatar">{user?.name?.slice(0, 1).toUpperCase() ?? 'U'}</div>
            <div className="profile-copy"><strong>{user?.name ?? 'Your account'}</strong><small>{user?.roles?.[0] ?? 'Member'}</small></div>
            <button className="icon-button profile-logout" onClick={() => void logout()} aria-label="Log out"><LogOut size={17} /></button>
          </div>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <button className="mobile-menu icon-button" onClick={() => setSidebarOpen(true)} aria-label="Open menu"><Menu size={21} /></button>
          <div className="breadcrumbs"><span>Workspace</span><span className="crumb-separator">/</span><strong>{title}</strong></div>
          <div className="topbar-actions">
            {user?.permissions.includes('transaction_history.view') && <form className="global-search" role="search" onSubmit={submitGlobalSearch}><Search size={16} /><input placeholder="Search transactions…" aria-label="Search transactions" value={globalSearch} onChange={(event) => setGlobalSearch(event.target.value)} /><kbd>↵</kbd></form>}
            <Link className="topbar-account" href="/settings" onClick={() => setSettingsOpen(true)} aria-label={`Account settings for ${user?.name ?? 'your account'}`}><span className="profile-avatar">{user?.name?.slice(0, 1).toUpperCase() ?? 'U'}</span><span>My account</span><ExternalArrow size={14} /></Link>
          </div>
        </header>
        <div className="content-area">{children}</div>
      </main>
    </div>
  );
}
