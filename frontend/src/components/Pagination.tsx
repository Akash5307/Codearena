// Codeforces-style numbered pager. Pages are 0-based internally.
export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;

  const current = page + 1; // 1-based for display
  const pages: number[] = [];
  const add = (p: number) => {
    if (p >= 1 && p <= totalPages && !pages.includes(p)) pages.push(p);
  };
  add(1);
  for (let p = current - 2; p <= current + 2; p++) add(p);
  add(totalPages);
  pages.sort((a, b) => a - b);

  const items: (number | "...")[] = [];
  let prev = 0;
  for (const p of pages) {
    if (prev && p - prev > 1) items.push("...");
    items.push(p);
    prev = p;
  }

  return (
    <div className="mt-3 flex items-center gap-1">
      <button
        className="cf-btn px-2 py-0.5"
        disabled={current <= 1}
        onClick={() => onChange(page - 1)}
      >
        ‹
      </button>
      {items.map((it, i) =>
        it === "..." ? (
          <span key={`e${i}`} className="px-2 text-gray-500">
            …
          </span>
        ) : (
          <button
            key={it}
            onClick={() => onChange(it - 1)}
            className={`min-w-[28px] rounded-sm border px-2 py-0.5 ${
              it === current
                ? "border-cf-blue bg-cf-blue font-bold text-white"
                : "border-cf-border bg-white hover:bg-gray-100"
            }`}
          >
            {it}
          </button>
        ),
      )}
      <button
        className="cf-btn px-2 py-0.5"
        disabled={current >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        ›
      </button>
    </div>
  );
}
