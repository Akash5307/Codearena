export function Spinner({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 py-8 text-gray-500">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-cf-border border-t-cf-blue" />
      {label}
    </div>
  );
}

export function ErrorBox({ message }: { message: string }) {
  return (
    <div className="my-3 border border-cf-red bg-red-50 px-3 py-2 text-cf-red">
      {message}
    </div>
  );
}
