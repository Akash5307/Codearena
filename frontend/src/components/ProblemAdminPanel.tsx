import { useRef, useState } from "react";
import { problemApi } from "../lib/services";
import type { ProblemDetail } from "../lib/types";
import { ApiError } from "../lib/api";
import { ErrorBox } from "./Spinner";

/**
 * Admin-only panel on the problem page: publish/unpublish and upload test cases.
 * Rendered only for PROBLEM_SETTER / ADMIN (the caller gates on role).
 */
export function ProblemAdminPanel({
  problem,
  onChanged,
}: {
  problem: ProblemDetail;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);
  const outputRef = useRef<HTMLInputElement>(null);
  const [isSample, setIsSample] = useState(false);

  async function togglePublish() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await problemApi.update(problem.id, { isPublished: !problem.isPublished });
      onChanged();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to update");
    } finally {
      setBusy(false);
    }
  }

  async function uploadTestCase(e: React.FormEvent) {
    e.preventDefault();
    const input = inputRef.current?.files?.[0];
    const output = outputRef.current?.files?.[0];
    if (!input || !output) {
      setError("Pick both an input and an output file.");
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await problemApi.uploadTestCase(problem.id, input, output, isSample);
      setNotice("Test case uploaded.");
      if (inputRef.current) inputRef.current.value = "";
      if (outputRef.current) outputRef.current.value = "";
      setIsSample(false);
      onChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="cf-panel border-cf-red">
      <div className="cf-panel-title bg-cf-red">Admin</div>
      <div className="space-y-3 p-3">
        {error && <ErrorBox message={error} />}
        {notice && <p className="text-[12px] text-verdict-ac">{notice}</p>}

        <div className="flex items-center justify-between">
          <span className="text-[12px]">
            Status:{" "}
            <b className={problem.isPublished ? "text-verdict-ac" : "text-gray-500"}>
              {problem.isPublished ? "Published" : "Draft"}
            </b>
          </span>
          <button className="cf-btn" onClick={togglePublish} disabled={busy}>
            {problem.isPublished ? "Unpublish" : "Publish"}
          </button>
        </div>

        <form onSubmit={uploadTestCase} className="space-y-2 border-t border-cf-border pt-3">
          <div className="text-[12px] font-bold text-gray-700">Upload test case</div>
          <label className="cf-label">Input file</label>
          <input ref={inputRef} type="file" className="block w-full text-[12px]" />
          <label className="cf-label">Expected output file</label>
          <input ref={outputRef} type="file" className="block w-full text-[12px]" />
          <label className="flex items-center gap-2 text-[12px]">
            <input type="checkbox" checked={isSample} onChange={(e) => setIsSample(e.target.checked)} />
            Sample (shown publicly on the problem page)
          </label>
          <button type="submit" className="cf-btn cf-btn-primary w-full" disabled={busy}>
            {busy ? "Working…" : "Upload test case"}
          </button>
        </form>
      </div>
    </div>
  );
}
