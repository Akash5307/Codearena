import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { submissionApi } from "../lib/services";
import type { Page, SubmissionListItem } from "../lib/types";
import { LANGUAGES, LANGUAGE_LABELS } from "../lib/types";
import { ApiError } from "../lib/api";
import { VerdictBadge } from "../components/VerdictBadge";
import { RatingName } from "../components/RatingName";
import { Pagination } from "../components/Pagination";
import { Spinner, ErrorBox } from "../components/Spinner";
import { formatDateTime, formatKb, formatMs } from "../lib/format";

const PAGE_SIZE = 30;
const VERDICTS = ["AC", "WA", "TLE", "RE", "CE", "PENDING"];

export function Status() {
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");
  const verdict = params.get("verdict") ?? "";
  const language = params.get("language") ?? "";
  const problemId = params.get("problemId") ?? "";

  const [data, setData] = useState<Page<SubmissionListItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    submissionApi
      .list({
        page,
        size: PAGE_SIZE,
        sort: "submittedAt,desc",
        verdict: verdict || undefined,
        language: language || undefined,
        problemId: problemId ? Number(problemId) : undefined,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load submissions"))
      .finally(() => setLoading(false));
  }, [page, verdict, language, problemId]);

  function setFilter(key: string, value: string) {
    const merged = new URLSearchParams(params);
    if (value) merged.set(key, value);
    else merged.delete(key);
    merged.delete("page");
    setParams(merged);
  }

  return (
    <div className="cf-panel">
      <div className="cf-panel-title flex items-center justify-between">
        <span>Status</span>
        <div className="flex items-center gap-2 text-[12px] font-normal">
          <select
            className="cf-input w-auto"
            value={verdict}
            onChange={(e) => setFilter("verdict", e.target.value)}
          >
            <option value="">All verdicts</option>
            {VERDICTS.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
          <select
            className="cf-input w-auto"
            value={language}
            onChange={(e) => setFilter("language", e.target.value)}
          >
            <option value="">All languages</option>
            {LANGUAGES.map((l) => (
              <option key={l} value={l}>
                {LANGUAGE_LABELS[l]}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="p-2">
        {problemId && (
          <div className="mb-2 text-[12px]">
            Filtered by problem #{problemId}.{" "}
            <button
              className="text-cf-blue hover:underline"
              onClick={() => setFilter("problemId", "")}
            >
              clear
            </button>
          </div>
        )}
        {loading ? (
          <Spinner />
        ) : error ? (
          <ErrorBox message={error} />
        ) : !data || data.content.length === 0 ? (
          <p className="p-4 text-gray-500">No submissions yet.</p>
        ) : (
          <>
            <table className="cf-table">
              <thead>
                <tr>
                  <th className="w-16">#</th>
                  <th className="w-40">When</th>
                  <th className="w-32">Who</th>
                  <th>Problem</th>
                  <th className="w-24">Lang</th>
                  <th className="w-40">Verdict</th>
                  <th className="w-20 text-right">Time</th>
                  <th className="w-24 text-right">Memory</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <Link to={`/submissions/${s.id}`} className="font-mono">
                        {s.id}
                      </Link>
                    </td>
                    <td className="text-[12px] text-gray-600">
                      {formatDateTime(s.submittedAt)}
                    </td>
                    <td>
                      <RatingName username={s.username} />
                    </td>
                    <td>{s.problemTitle}</td>
                    <td className="text-[12px]">
                      {LANGUAGE_LABELS[s.language] ?? s.language}
                    </td>
                    <td>
                      <Link to={`/submissions/${s.id}`} className="no-underline">
                        <VerdictBadge verdict={s.verdict} />
                      </Link>
                    </td>
                    <td className="text-right text-[12px]">{formatMs(s.timeUsedMs)}</td>
                    <td className="text-right text-[12px]">{formatKb(s.memoryUsedKb)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination
              page={data.number}
              totalPages={data.totalPages}
              onChange={(p) => {
                const merged = new URLSearchParams(params);
                merged.set("page", String(p));
                setParams(merged);
              }}
            />
          </>
        )}
      </div>
    </div>
  );
}
