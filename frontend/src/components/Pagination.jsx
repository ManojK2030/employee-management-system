/** Builds [0, '…', 4, 5, 6, '…', 19] style page lists so large page counts stay compact. */
export function pageWindow(current, total, radius = 1) {
  const pages = new Set([0, total - 1]);
  for (let i = current - radius; i <= current + radius; i += 1) {
    if (i >= 0 && i < total) pages.add(i);
  }
  const sorted = [...pages].sort((a, b) => a - b);

  const result = [];
  sorted.forEach((page, index) => {
    if (index > 0 && page - sorted[index - 1] > 1) result.push('…');
    result.push(page);
  });
  return result;
}

export default function Pagination({ page, totalPages, totalElements, size, onPageChange }) {
  if (totalElements === 0) return null;

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <nav className="pagination" aria-label="Pagination">
      <span className="pagination-summary">
        Showing {from}–{to} of {totalElements}
      </span>

      <div className="pagination-controls">
        <button type="button" className="btn btn-sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </button>

        {pageWindow(page, totalPages).map((item, index) =>
          item === '…' ? (
            // eslint-disable-next-line react/no-array-index-key
            <span key={`gap-${index}`} className="pagination-gap">…</span>
          ) : (
            <button
              key={item}
              type="button"
              className={`btn btn-sm page-number ${item === page ? 'btn-primary' : ''}`}
              aria-current={item === page ? 'page' : undefined}
              onClick={() => onPageChange(item)}
            >
              {item + 1}
            </button>
          ),
        )}

        <button
          type="button"
          className="btn btn-sm"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </nav>
  );
}
