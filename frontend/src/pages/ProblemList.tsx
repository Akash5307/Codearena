import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { problemApi } from "../lib/services";
import type { Page, ProblemListItem, Tag } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { canSetProblems } from "../lib/format";
import { DifficultyBadge } from "../components/DifficultyBadge";
import { RatingName } from "../components/RatingName";
import { Pagination } from "../components/Pagination";
import { Spinner, ErrorBox } from "../components/Spinner";

const PAGE_SIZE = 25;

export function ProblemList() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const page = Number(params.get("page") ?? "0");
  const title = params.get("title") ?? "";
  const difficulty = params.get("difficulty") ?? "";
  const tag = params.get("tag") ?? "";

  const [data, setData] = useState<Page<ProblemListItem> | null>(null);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // local controlled search box (applied on submit)
  const [titleInput, setTitleInput] = useState(title);

  useEffect(() => {
    problemApi.tags().then(setTags).catch(() => setTags([]));
  }, []);

  useEffect(() => {
    setLoading(true);
    setError(null);
    problemApi
      .list({
        page,
        size: PAGE_SIZE,
        sort: "id,asc",
        title: title || undefined,
        difficulty: difficulty || undefined,
        tag: tag || undefined,
      })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load problems"))
      .finally(() => setLoading(false));
  }, [page, title, difficulty, tag]);

  function update(next: Record<string, string>) {
    const merged = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) {
      if (v) merged.set(k, v);
      else merged.delete(k);
    }
    merged.delete("page"); // reset to first page on filter change
    setParams(merged);
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_220px]">
      <div className="cf-panel">
        <div className="cf-panel-title flex items-center justify-between">
          <span>Problemset</span>
          {canSetProblems(user?.role) && (
            <Link
              to="/problems/new"
              className="rounded-sm bg-white/80 px-2 py-0.5 text-[12px] font-bold text-cf-blue no-underline hover:bg-white"
            >
              + New problem
            </Link>
          )}
        </div>
        <div className="p-2">
          {loading ? (
            <Spinner />
          ) : error ? (
            <ErrorBox message={error} />
          ) : !data || data.content.length === 0 ? (
            <p className="p-4 text-gray-500">No problems found.</p>
          ) : (
            <>
              <table className="cf-table">
                <thead>
                  <tr>
                    <th className="w-12">#</th>
                    <th>Name</th>
                    <th className="w-24">Difficulty</th>
                    <th className="w-28">Author</th>
                    <th className="w-20 text-center">Limits</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((p) => (
                    <tr key={p.id}>
                      <td className="text-gray-500">{p.id}</td>
                      <td>
                        <Link to={`/problems/${p.slug}`} className="font-bold">
                          {p.title}
                        </Link>
                        {p.tags.length > 0 && (
                          <div className="mt-0.5 flex flex-wrap gap-1">
                            {p.tags.map((t) => (
                              <button
                                key={t}
                                onClick={() => update({ tag: t })}
                                className="rounded-sm bg-cf-panel px-1.5 py-0.5 text-[11px] text-gray-600 hover:bg-gray-200"
                              >
                                {t}
                              </button>
                            ))}
                          </div>
                        )}
                      </td>
                      <td>
                        <DifficultyBadge difficulty={p.difficulty} />
                      </td>
                      <td>
                        <RatingName username={p.authorUsername} />
                      </td>
                      <td className="text-center text-[11px] text-gray-600">
                        {p.timeLimitMs / 1000}s / {p.memoryLimitMb}MB
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

      {/* Filter sidebar */}
      <aside className="space-y-4">
        <div className="cf-panel">
          <div className="cf-panel-title">Search</div>
          <form
            className="space-y-2 p-3"
            onSubmit={(e) => {
              e.preventDefault();
              update({ title: titleInput.trim() });
            }}
          >
            <input
              className="cf-input"
              placeholder="Problem name…"
              value={titleInput}
              onChange={(e) => setTitleInput(e.target.value)}
            />
            <button className="cf-btn cf-btn-primary w-full">Search</button>
          </form>
        </div>

        <div className="cf-panel">
          <div className="cf-panel-title">Difficulty</div>
          <div className="flex flex-col p-2">
            {["EASY", "MEDIUM", "HARD"].map((d) => (
              <button
                key={d}
                onClick={() => update({ difficulty: difficulty === d ? "" : d })}
                className={`px-2 py-1 text-left hover:bg-cf-panel ${
                  difficulty === d ? "font-bold" : ""
                }`}
              >
                {d.charAt(0) + d.slice(1).toLowerCase()}
                {difficulty === d && " ✓"}
              </button>
            ))}
          </div>
        </div>

        {tags.length > 0 && (
          <div className="cf-panel">
            <div className="cf-panel-title">Tags</div>
            <div className="flex flex-wrap gap-1 p-2">
              {tags.map((t) => (
                <button
                  key={t.id}
                  onClick={() => update({ tag: tag === t.name ? "" : t.name })}
                  className={`rounded-sm border px-1.5 py-0.5 text-[11px] ${
                    tag === t.name
                      ? "border-cf-blue bg-cf-blue text-white"
                      : "border-cf-border bg-cf-panel text-gray-700 hover:bg-gray-200"
                  }`}
                >
                  {t.name}
                </button>
              ))}
            </div>
          </div>
        )}

        {(title || difficulty || tag) && (
          <button
            onClick={() => {
              setTitleInput("");
              setParams(new URLSearchParams());
            }}
            className="text-cf-blue hover:underline"
          >
            Clear all filters
          </button>
        )}
      </aside>
    </div>
  );
}
