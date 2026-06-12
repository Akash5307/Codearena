import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { contestApi } from "../lib/services";
import type { ContestListItem, Page } from "../lib/types";
import { ApiError } from "../lib/api";
import { Pagination } from "../components/Pagination";
import { RatingName } from "../components/RatingName";
import { Spinner, ErrorBox } from "../components/Spinner";
import { useAuth } from "../store/auth";
import {
  CONTEST_STATE_LABELS,
  contestStateClass,
  formatCountdown,
  formatDateTime,
  formatDuration,
  isAdmin,
} from "../lib/format";

const PAGE_SIZE = 25;

const TABS: { key: string; label: string }[] = [
  { key: "", label: "All" },
  { key: "upcoming", label: "Upcoming" },
  { key: "running", label: "Running" },
  { key: "past", label: "Past" },
];

export function ContestList() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const status = params.get("status") ?? "";
  const page = Number(params.get("page") ?? "0");

  const [data, setData] = useState<Page<ContestListItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    contestApi
      .list({ page, size: PAGE_SIZE, sort: "startTime,desc", status: status || undefined })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load contests"))
      .finally(() => setLoading(false));
  }, [status, page]);

  function setTab(key: string) {
    const next = new URLSearchParams();
    if (key) next.set("status", key);
    setParams(next);
  }

  return (
    <div className="cf-panel">
      <div className="cf-panel-title flex items-center justify-between">
        <span>Contests</span>
        <div className="flex items-center gap-1">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`rounded-sm px-2 py-0.5 text-[12px] ${
                status === t.key
                  ? "bg-cf-blue text-white"
                  : "bg-white/70 text-cf-blue hover:bg-white"
              }`}
            >
              {t.label}
            </button>
          ))}
          {isAdmin(user?.role) && (
            <Link
              to="/contests/new"
              className="ml-2 rounded-sm bg-white/80 px-2 py-0.5 text-[12px] font-bold text-cf-blue no-underline hover:bg-white"
            >
              + New contest
            </Link>
          )}
        </div>
      </div>

      <div className="p-2">
        {loading ? (
          <Spinner />
        ) : error ? (
          <ErrorBox message={error} />
        ) : !data || data.content.length === 0 ? (
          <p className="p-4 text-gray-500">No contests here yet.</p>
        ) : (
          <>
            <table className="cf-table">
              <thead>
                <tr>
                  <th>Contest</th>
                  <th className="w-24">Status</th>
                  <th className="w-44">Start</th>
                  <th className="w-20 text-center">Length</th>
                  <th className="w-28">Author</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <Link to={`/contests/${c.slug}`} className="font-bold">
                        {c.title}
                      </Link>
                      <span className="ml-2 text-[11px] text-gray-500">{c.type}</span>
                      {c.isRated && (
                        <span className="ml-1 text-[11px] text-cf-red">• rated</span>
                      )}
                    </td>
                    <td>
                      <span className={contestStateClass(c.state)}>
                        {CONTEST_STATE_LABELS[c.state] ?? c.state}
                      </span>
                      {c.state === "BEFORE" && (
                        <div className="text-[11px] text-gray-500">
                          in {formatCountdown(c.startTime)}
                        </div>
                      )}
                    </td>
                    <td className="text-[12px] text-gray-700">
                      {formatDateTime(c.startTime)}
                    </td>
                    <td className="text-center text-[12px] text-gray-600">
                      {formatDuration(c.durationMinutes)}
                    </td>
                    <td>
                      <RatingName username={c.authorUsername} />
                    </td>
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
