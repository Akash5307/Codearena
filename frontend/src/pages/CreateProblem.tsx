import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { problemApi } from "../lib/services";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { canSetProblems } from "../lib/format";
import { ErrorBox } from "../components/Spinner";

const DIFFICULTIES = ["EASY", "MEDIUM", "HARD"];

export function CreateProblem() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [statement, setStatement] = useState("");
  const [inputFormat, setInputFormat] = useState("");
  const [outputFormat, setOutputFormat] = useState("");
  const [difficulty, setDifficulty] = useState("EASY");
  const [timeLimitMs, setTimeLimitMs] = useState(2000);
  const [memoryLimitMb, setMemoryLimitMb] = useState(256);
  const [tags, setTags] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!canSetProblems(user?.role)) {
    return (
      <div className="cf-panel p-8 text-center text-gray-600">
        You need the <b>PROBLEM_SETTER</b> or <b>ADMIN</b> role to create problems.
      </div>
    );
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !statement.trim()) {
      setError("Title and statement are required.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const created = await problemApi.create({
        title: title.trim(),
        statement: statement.trim(),
        inputFormat: inputFormat.trim() || undefined,
        outputFormat: outputFormat.trim() || undefined,
        difficulty,
        timeLimitMs,
        memoryLimitMb,
        tags: tags
          .split(",")
          .map((t) => t.trim())
          .filter(Boolean),
      });
      // Land on the detail page, where the admin panel handles test cases + publish.
      navigate(`/problems/${created.slug}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create problem");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-[760px]">
      <div className="cf-panel">
        <div className="cf-panel-title flex items-center justify-between">
          <span>New problem</span>
          <Link to="/problems" className="text-[12px] font-normal text-white/90 hover:underline">
            ← back to problemset
          </Link>
        </div>
        <form onSubmit={onSubmit} className="space-y-3 p-4">
          {error && <ErrorBox message={error} />}

          <div>
            <label className="cf-label">Title</label>
            <input className="cf-input" value={title} onChange={(e) => setTitle(e.target.value)} autoFocus required />
          </div>

          <div>
            <label className="cf-label">Statement (Markdown)</label>
            <textarea
              className="cf-input font-mono text-[13px]"
              rows={8}
              value={statement}
              onChange={(e) => setStatement(e.target.value)}
              placeholder="Describe the problem. Markdown + $LaTeX$ supported."
              required
            />
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label className="cf-label">Input format</label>
              <textarea className="cf-input" rows={3} value={inputFormat} onChange={(e) => setInputFormat(e.target.value)} />
            </div>
            <div>
              <label className="cf-label">Output format</label>
              <textarea className="cf-input" rows={3} value={outputFormat} onChange={(e) => setOutputFormat(e.target.value)} />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <div>
              <label className="cf-label">Difficulty</label>
              <select className="cf-input" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
                {DIFFICULTIES.map((d) => (
                  <option key={d} value={d}>
                    {d.charAt(0) + d.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="cf-label">Time (ms)</label>
              <input type="number" min={100} step={100} className="cf-input" value={timeLimitMs} onChange={(e) => setTimeLimitMs(Number(e.target.value))} />
            </div>
            <div>
              <label className="cf-label">Memory (MB)</label>
              <input type="number" min={16} step={16} className="cf-input" value={memoryLimitMb} onChange={(e) => setMemoryLimitMb(Number(e.target.value))} />
            </div>
            <div>
              <label className="cf-label">Tags (comma-sep)</label>
              <input className="cf-input" value={tags} onChange={(e) => setTags(e.target.value)} placeholder="dp, greedy" />
            </div>
          </div>

          <div className="flex items-center gap-3 pt-1">
            <button type="submit" className="cf-btn cf-btn-primary" disabled={submitting}>
              {submitting ? "Creating…" : "Create problem"}
            </button>
            <span className="text-[12px] text-gray-500">
              Created as a draft — add test cases and publish on the next screen.
            </span>
          </div>
        </form>
      </div>
    </div>
  );
}
