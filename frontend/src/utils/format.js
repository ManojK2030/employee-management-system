const currency = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
});

export function formatCurrency(value) {
  if (value === null || value === undefined || value === '') return '-';
  return currency.format(Number(value));
}

/** "2024-07-01" -> "01 Jul 2024". Built from the parts so time zones can never shift the day. */
export function formatDate(isoDate) {
  if (!isoDate) return '-';
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}
