import { useCallback, useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { contestApi } from "../lib/services";
import type { ContestDetail as Contest, Page, Standings, SubmissionListItem } from "../lib/types";
import { LANGUAGE_LABELS } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { RatingName } from "../components/RatingName";
import { VerdictBadge } from "../components/VerdictBadge";
import { Pagination } from "../components/Pagination";
import { Spinner, ErrorBox } from "../components/Spinner";
import {
  CONTEST_STATE_LABELS,
  contestStateClass,
  formatCountdown,
  formatDateTime,
  formatDuration,
} from "../lib/format";

type Tab = "problems" | "standings" | "mine";

export function ContestDetail() {
  const { slug = "" } = useParams();
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const tabParam = params.get("tab");
  const tab: Tab = tabParam === "standings" ? "standings" : tabParam === "mine" ? "mine" : "problems";

  const [contest, setContest] = useState<Contest | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [registered, setRegistered] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [registerMsg, setRegisterMsg] = useState<string | null>(null);

  // re-render every second so the countdown ticks
  const [, setTick] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setTick((n) => n + 1), 1000);
    return () => clearInterval(t);
  }, []);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    contestApi
      .get(slug)
      .then(setContest)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load contest"))
      .finally(() => setLoading(false));
  }, [slug]);

  useEffect(() => {
    load();
    setRegistered(false);
    setRegisterMsg(null);
  }, [load]);

  async function onRegister() {
    if (!contest) return;
    setRegistering(true);
    setRegisterMsg(null);
    try {
      await contestApi.register(contest.id);
      setRegistered(true);
      setRegisterMsg("You are registered for this contest.");
      load(); // refresh registration count
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : "Registration failed";
      // "already registered" is a success state from the user's point of view
      if (/already/i.test(msg)) {
        setRegistered(true);
        setRegisterMsg("You are already registered.");
      } else {
        setRegisterMsg(msg);
      }
    } finally {
      setRegistering(false);
    }
  }

  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!contest) return null;

  const countdown =
    contest.state === "BEFORE"
      ? { label: "Starts in", value: formatCountdown(contest.startTime) }
      : contest.state === "RUNNING"
        ? { label: "Ends in", value: formatCountdown(contest.endTime) }
        : null;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="cf-panel p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-[22px] font-bold">{contest.title}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-[12px] text-gray-600">
              <span className={contestStateClass(contest.state)}>
                {CONTEST_STATE_LABELS[contest.state] ?? contest.state}
              </span>
              <span>•</span>
              <span>{contest.type}</span>
              {contest.isRated && <span className="text-cf-red">• rated</span>}
              <span>•</span>
              <span>by <RatingName username={contest.authorUsername} /></span>
              <span>•</span>
              <span>{contest.registrationCount} registered</span>
            </div>
          </div>
          {countdown && (
            <div className="text-right">
              <div className="text-[11px] uppercase tracking-wide text-gray-500">
                {countdown.label}
              </div>
              <div className="font-mono text-[20px] font-bold text-cf-blue">
                {countdown.value}
              </div>
            </div>
          )}
        </div>

        <div className="mt-3 grid grid-cols-2 gap-2 text-[12px] text-gray-600 sm:grid-cols-4">
          <div>
            <div className="text-gray-400">Start</div>
            <div>{formatDateTime(contest.startTime)}</div>
          </div>
          <div>
            <div className="text-gray-400">End</div>
            <div>{formatDateTime(contest.endTime)}</div>
          </div>
          <div>
            <div className="text-gray-400">Duration</div>
            <div>{formatDuration(contest.durationMinutes)}</div>
          </div>
          <div>
            <div className="text-gray-400">Problems</div>
            <div>{contest.problems.length}</div>
          </div>
        </div>

        {/* Register */}
        <div className="mt-4 flex items-center gap-3">
          {!user ? (
            <p className="text-[13px] text-gray-600">
              <Link to="/login" className="font-bold">Enter</Link> to register.
            </p>
          ) : contest.state === "ENDED" ? (
            <span className="text-[13px] text-gray-500">This contest has finished.</span>
          ) : registered ? (
            <span className="text-[13px] font-bold text-verdict-ac">✓ Registered</span>
          ) : (
            <button
              className="cf-btn cf-btn-primary"
              onClick={onRegister}
              disabled={registering}
            >
              {registering ? "Registering…" : "Register"}
            </button>
          )}
          {registerMsg && <span className="text-[12px] text-gray-600">{registerMsg}</span>}
        </div>

        {contest.description && (
          <p className="mt-3 whitespace-pre-wrap text-[13px] text-gray-700">
            {contest.description}
          </p>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-cf-border">
        {(["problems", "standings", ...(user ? ["mine" as Tab] : [])] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => {
              const next = new URLSearchParams(params);
              if (t === "problems") next.delete("tab");
              else next.set("tab", t);
              setParams(next);
            }}
            className={`-mb-px border-b-2 px-4 py-2 text-[13px] font-bold ${
              tab === t
                ? "border-cf-blue text-cf-blue"
                : "border-transparent text-gray-500 hover:text-cf-blue"
            }`}
          >
            {t === "problems" ? "Problems" : t === "standings" ? "Standings" : "My submissions"}
          </button>
        ))}
      </div>

      {tab === "problems" ? (
        <ProblemsTab contest={contest} />
      ) : tab === "standings" ? (
        <StandingsTab contestId={contest.id} />
      ) : (
        <MySubmissionsTab contestId={contest.id} />
      )}
    </div>
  );
}

function MySubmissionsTab({ contestId }: { contestId: number }) {
  const [data, setData] = useState<Page<SubmissionListItem> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    contestApi
      .mySubmissions(contestId, { page, size: 20, sort: "submittedAt,desc" })
      .then(setData)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load submissions"))
      .finally(() => setLoading(false));
  }, [contestId, page]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!data || data.content.length === 0) {
    return (
      <div className="cf-panel p-6 text-center text-gray-500">
        You haven't submitted in this contest yet.
      </div>
    );
  }

  return (
    <div className="cf-panel">
      <div className="cf-panel-title">My submissions</div>
      <div className="p-2">
        <table className="cf-table">
          <thead>
            <tr>
              <th className="w-16">#</th>
              <th>Problem</th>
              <th className="w-24">Lang</th>
              <th className="w-36">Verdict</th>
              <th className="w-40">When</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map((s) => (
              <tr key={s.id}>
                <td>
                  <Link to={`/submissions/${s.id}`} className="font-mono">{s.id}</Link>
                </td>
                <td>{s.problemTitle}</td>
                <td className="text-[12px]">{LANGUAGE_LABELS[s.language] ?? s.language}</td>
                <td><VerdictBadge verdict={s.verdict} /></td>
                <td className="text-[12px] text-gray-600">{formatDateTime(s.submittedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
      </div>
    </div>
  );
}

function ProblemsTab({ contest }: { contest: Contest }) {
  const canSolve = contest.state !== "BEFORE";
  return (
    <div className="cf-panel">
      <div className="cf-panel-title">Problems</div>
      <div className="p-2">
        {contest.problems.length === 0 ? (
          <p className="p-4 text-gray-500">No problems in this contest.</p>
        ) : (
          <table className="cf-table">
            <thead>
              <tr>
                <th className="w-12">#</th>
                <th>Name</th>
                <th className="w-20 text-center">Points</th>
              </tr>
            </thead>
            <tbody>
              {[...contest.problems]
                .sort((a, b) => a.orderIndex - b.orderIndex)
                .map((p) => (
                  <tr key={p.problemId}>
                    <td className="font-bold text-cf-blue">{p.label}</td>
                    <td>
                      {canSolve ? (
                        <Link
                          to={`/problems/${p.problemSlug}?contest=${contest.id}`}
                          className="font-bold"
                        >
                          {p.problemTitle}
                        </Link>
                      ) : (
                        <span className="text-gray-500">{p.problemTitle}</span>
                      )}
                    </td>
                    <td className="text-center text-gray-600">{p.points ?? "—"}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        )}
        {contest.state === "BEFORE" && (
          <p className="p-3 text-[12px] text-gray-500">
            Problems become solvable when the contest starts.
          </p>
        )}
      </div>
    </div>
  );
}

function StandingsTab({ contestId }: { contestId: number }) {
  const [standings, setStandings] = useState<Standings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    contestApi
      .standings(contestId)
      .then(setStandings)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load standings"))
      .finally(() => setLoading(false));
  }, [contestId]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!standings || standings.entries.length === 0) {
    return (
      <div className="cf-panel p-6 text-center text-gray-500">
        No submissions yet — standings will appear once contestants start solving.
      </div>
    );
  }

  // problem labels from the first row (every row carries the same set)
  const labels = standings.entries[0].problemResults.map((r) => r.label);

  return (
    <div className="cf-panel">
      <div className="cf-panel-title">Standings</div>
      <div className="overflow-x-auto p-2">
        <table className="cf-table text-center">
          <thead>
            <tr>
              <th className="w-12">#</th>
              <th className="text-left">Who</th>
              <th className="w-14">=</th>
              <th className="w-20">Penalty</th>
              {labels.map((l) => (
                <th key={l} className="w-14">{l}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {standings.entries.map((e) => (
              <tr key={e.userId}>
                <td className="text-gray-500">{e.rank}</td>
                <td className="text-left">
                  <RatingName username={e.username} />
                </td>
                <td className="font-bold">{e.solvedCount}</td>
                <td className="text-gray-600">{e.penaltyMinutes}</td>
                {e.problemResults.map((r) => (
                  <td
                    key={r.label}
                    className={
                      r.solved
                        ? "bg-green-50 font-bold text-verdict-ac"
                        : r.attempts > 0
                          ? "bg-red-50 text-verdict-wa"
                          : "text-gray-300"
                    }
                  >
                    {r.solved ? (
                      <>
                        +{r.attempts > 0 ? r.attempts : ""}
                        <div className="text-[10px] font-normal text-gray-500">
                          {r.solvedAtMinute}
                        </div>
                      </>
                    ) : r.attempts > 0 ? (
                      `-${r.attempts}`
                    ) : (
                      "·"
                    )}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
