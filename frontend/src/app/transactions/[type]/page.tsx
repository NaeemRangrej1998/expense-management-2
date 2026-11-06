import TransactionsView from './TransactionsView';

export const instant = false;

export function generateStaticParams() {
  return [{ type: 'income' }, { type: 'expense' }, { type: 'history' }];
}

export default async function TransactionsPage({
  params,
  searchParams,
}: {
  params: Promise<{ type: string }>;
  searchParams: Promise<{ search?: string | string[] }>;
}) {
  const [{ type }, query] = await Promise.all([params, searchParams]);
  const initialSearch = typeof query.search === 'string' ? query.search : '';
  return <TransactionsView key={`${type}:${initialSearch}`} type={type} initialSearch={initialSearch} />;
}
