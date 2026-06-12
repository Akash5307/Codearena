import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { contestApi, problemApi } from "../lib/services";
import type { ProblemListItem } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { isAdmin } from "../lib/format";
import { ErrorBox } from "../components/Spinner";

const TYPES = ["ICPC", "IOI", "EDUCATIONAL"];

interface Row {
  problemId: number | "";
  label: string;
  points: number;
}

// default labels A, B, C, … for new rows
const labelFor = (i: number) => String.fromCharCode(65 + i);

export function CreateContest() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState("ICPC");
  // datetime-local default: 1 hour from now, rounded to the minute
  const [startTime, setStartTime] = useState(() => {
    const d = new Date(Date.now() + 60 * 60 * 1000);
    d.setSeconds(0, 0);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  });
  const [durationMinutes, setDurationMinutes] = useState(120);
  const [isRated, setIsRated] = useState(true);
  const [rows, setRows] = useState<Row[]>([{ problemId: "", label: "A", points: 100 }]);

  const [problems, setProblems] = useState<ProblemListItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    problemApi
      .list({ page: 0, size: 200, sort: "id,asc" })
      .then((p) => setProblems(p.content))
      .catch(() => setProblems([]));
  }, []);

  if (!isAdmin(user?.role)) {
    return (
      <div className="cf-panel p-8 text-center text-gray-600">
        You need the <b>ADMIN</b> role to create contests.
      </div>
    );
  }

  function addRow() {
    setRows((r) => [...r, { problemId: "", label: labelFor(r.length), points: 100 }]);
  }
  function removeRow(i: number) {
    setRows((r) => r.filter((_, idx) => idx !== i));
  }
  function updateRow(i: number, patch: Partial<Row>) {
    setRows((r) => r.map((row, idx) => (idx === i ? { ...row, ...patch } : row)));
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) {
      setError("Title is required.");
      return;
    }
    const chosen = rows.filter((r) => r.problemId !== "");
    if (chosen.length === 0) {
      setError("Add at least one problem.");
      return;
    }
    const labels = chosen.map((r) => r.label.trim());
    if (new Set(labels).size !== labels.length) {
      setError("Problem labels must be unique.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const created = await contestApi.create({
        title: title.trim(),
        description: description.trim() || undefined,
        type,
        // datetime-local has no zone; append :00 seconds — backend reads it as local.
        startTime: `${startTime}:00`,
        durationMinutes,
        isRated,
        problems: chosen.map((r, idx) => ({
          problemId: Number(r.problemId),
          label: r.label.trim(),
          orderIndex: idx,
          points: r.points,
        })),
      });
      navigate(`/contests/${created.slug}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create contest");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-[760px]">
      <div className="cf-panel">
        <div className="cf-panel-title flex items-center justify-between">
          <span>New contest</span>
          <Link to="/contests" className="text-[12px] font-normal text-white/90 hover:underline">
            ← back to contests
          </Link>
        </div>
        <form onSubmit={onSubmit} className="space-y-3 p-4">
          {error && <ErrorBox message={error} />}

          <div>
            <label className="cf-label">Title</label>
            <input className="cf-input" value={title} onChange={(e) => setTitle(e.target.value)} autoFocus required />
          </div>

          <div>
            <label className="cf-label">Description</label>
            <textarea className="cf-input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>

          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <div>
              <label className="cf-label">Type</label>
              <select className="cf-input" value={type} onChange={(e) => setType(e.target.value)}>
                {TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div className="col-span-2">
              <label className="cf-label">Start time</label>
              <input type="datetime-local" className="cf-input" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
            </div>
            <div>
              <label className="cf-label">Duration (min)</label>
              <input type="number" min={1} className="cf-input" value={durationMinutes} onChange={(e) => setDurationMinutes(Number(e.target.value))} />
            </div>
          </div>

          <label className="flex items-center gap-2 text-[13px]">
            <input type="checkbox" checked={isRated} onChange={(e) => setIsRated(e.target.checked)} />
            Rated contest
          </label>

          {/* Problems */}
          <div className="border-t border-cf-border pt-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="font-bold">Problems</span>
              <button type="button" className="cf-btn" onClick={addRow}>+ add problem</button>
            </div>
            {problems.length === 0 && (
              <p className="text-[12px] text-gray-500">
                No published problems found — create and publish a problem first.
              </p>
            )}
            <div className="space-y-2">
              {rows.map((row, i) => (
                <div key={i} className="flex items-center gap-2">
                  <input
                    className="cf-input w-16 text-center"
                    value={row.label}
                    onChange={(e) => updateRow(i, { label: e.target.value })}
                    title="Label"
                  />
                  <select
                    className="cf-input flex-1"
                    value={row.problemId}
                    onChange={(e) => updateRow(i, { problemId: e.target.value === "" ? "" : Number(e.target.value) })}
                  >
                    <option value="">— select problem —</option>
                    {problems.map((p) => (
                      <option key={p.id} value={p.id}>
                        #{p.id} {p.title}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    className="cf-input w-20"
                    value={row.points}
                    onChange={(e) => updateRow(i, { points: Number(e.target.value) })}
                    title="Points"
                  />
                  <button type="button" className="px-2 text-cf-red hover:underline" onClick={() => removeRow(i)} title="Remove">
                    ✕
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center gap-3 pt-1">
            <button type="submit" className="cf-btn cf-btn-primary" disabled={submitting}>
              {submitting ? "Creating…" : "Create contest"}
            </button>
            <span className="text-[12px] text-gray-500">
              Start time must be in the future; users register before it begins.
            </span>
          </div>
        </form>
      </div>
    </div>
  );
}
