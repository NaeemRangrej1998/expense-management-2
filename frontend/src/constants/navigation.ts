// Centralized navigation configuration
// This single source drives both the sidebar and breadcrumbs

export interface NavItem {
  label: string;
  href?: string;
  icon: string;
  permission?: string;
  children?: NavItem[];
}

export const navigation: NavItem[] = [
  {
    label: 'Dashboard',
    href: '/dashboard',
    icon: 'LayoutDashboard',
    permission: 'dashboard.view',
  },
  {
    label: 'Transactions',
    icon: 'ArrowLeftRight',
    children: [
      {
        label: 'Income',
        href: '/transactions/income',
        icon: 'TrendingUp',
        permission: 'income.view',
      },
      {
        label: 'Expense',
        href: '/transactions/expense',
        icon: 'TrendingDown',
        permission: 'expense.view',
      },
    ],
  },
  {
    label: 'Transaction History',
    href: '/transaction-history',
    icon: 'History',
    permission: 'transaction_history.view',
  },
  {
    label: 'Reports',
    href: '/reports',
    icon: 'FileText',
    permission: 'reports.view',
  },
  {
    label: 'Settings',
    icon: 'Settings',
    children: [
      {
        label: 'Account settings',
        href: '/settings',
        icon: 'Settings',
      },
      {
        label: 'Users',
        href: '/settings/users',
        icon: 'Users',
        permission: 'users.view',
      },
      {
        label: 'Roles',
        href: '/settings/roles',
        icon: 'ShieldCheck',
        permission: 'roles.view',
      },
      {
        label: 'Permissions',
        href: '/settings/permissions',
        icon: 'KeyRound',
        permission: 'permissions.view',
      },
      {
        label: 'Areas',
        href: '/settings/areas',
        icon: 'MapPin',
        permission: 'areas.view',
      },
    ],
  },
];
