// Central type definitions for the application

export type UserStatus = 'ACTIVE' | 'INACTIVE';
export type AreaStatus = 'ACTIVE' | 'INACTIVE';
export type TransactionType = 'INCOME' | 'EXPENSE';
export type TransactionStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CHEQUE' | 'UPI' | 'CARD' | 'OTHER';

export interface ApiError {
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AuthUser {
  userId: number;
  name: string;
  email: string;
  roles: string[];
  permissions: string[];
  areas: { id: number; name: string }[];
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  mobileNumber?: string;
  status: UserStatus;
  roles: { id: number; name: string }[];
  areas: { id: number; name: string }[];
  createdAt: string;
  updatedAt: string;
}

export interface RoleResponse {
  id: number;
  name: string;
  description?: string;
  systemRole: boolean;
  permissions: PermissionResponse[];
  createdAt: string;
}

export interface PermissionResponse {
  id: number;
  name: string;
  description?: string;
  module: string;
  action: string;
}

export interface AreaResponse {
  id: number;
  name: string;
  description?: string;
  status: AreaStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryInfo {
  id: number;
  name: string;
}

export interface TransactionResponse {
  id: number;
  transactionNumber: string;
  type: TransactionType;
  amount: number;
  incomeCategory?: CategoryInfo;
  expenseCategory?: CategoryInfo;
  area?: { id: number; name: string };
  createdBy: { id: number; name: string; email: string };
  transactionDate: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  description?: string;
  status: TransactionStatus;
  approvedBy?: { id: number; name: string; email: string };
  approvedAt?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardSummary {
  totalIncome: number;
  totalExpenses: number;
  remainingBalance: number;
  startDate?: string;
  endDate?: string;
}

export interface ExpenseCategoryBreakdown {
  category: string;
  amount: number;
  percentage: number;
}

export type DateRangeFilter = 'today' | 'week' | 'month' | 'year' | 'custom';
