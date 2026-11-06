import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { format, parseISO } from 'date-fns';
import { DateRangeFilter } from '@/types';
import { startOfDay, endOfDay, startOfWeek, endOfWeek, startOfMonth, endOfMonth, startOfYear, endOfYear } from 'date-fns';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number | string | null | undefined): string {
  const num = Number(amount ?? 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(num);
}

export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    return format(parseISO(dateStr), 'dd MMM yyyy');
  } catch {
    return dateStr;
  }
}

export function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    return format(parseISO(dateStr), 'dd MMM yyyy, hh:mm a');
  } catch {
    return dateStr;
  }
}

export function getDateRange(filter: DateRangeFilter): { startDate: Date; endDate: Date } {
  const now = new Date();
  switch (filter) {
    case 'today':
      return { startDate: startOfDay(now), endDate: endOfDay(now) };
    case 'week':
      return { startDate: startOfWeek(now, { weekStartsOn: 1 }), endDate: endOfWeek(now, { weekStartsOn: 1 }) };
    case 'month':
      return { startDate: startOfMonth(now), endDate: endOfMonth(now) };
    case 'year':
      return { startDate: startOfYear(now), endDate: endOfYear(now) };
    default:
      return { startDate: startOfDay(now), endDate: endOfDay(now) };
  }
}

export function formatApiDate(date: Date): string {
  return format(date, 'yyyy-MM-dd');
}
