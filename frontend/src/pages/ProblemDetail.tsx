import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import { problemApi, submissionApi } from "../lib/services";
import type { ProblemDetail as Problem, SampleTestCaseContent } from "../lib/types";
import { LANGUAGES, LANGUAGE_LABELS, type Language } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { DifficultyBadge } from "../components/DifficultyBadge";
import { RatingName } from "../components/RatingName";
import { CodeEditor } from "../components/CodeEditor";
import { Spinner, ErrorBox } from "../components/Spinner";
import { ProblemAdminPanel } from "../components/ProblemAdminPanel";
import { canSetProblems } from "../lib/format";

const LANG_STORAGE_KEY = "ca_lang";

export function ProblemDetail() {
  const { slug = "" } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const contestParam = searchParams.get("contest");
  const contestId = contestParam ? Number(contestParam) : null;

  const [problem, setProblem] = useState<Problem | null>(null);
  const [samples, setSamples] = useState<SampleTestCaseContent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [language, setLanguage] = useState<Language>(
    (localStorage.getItem(LANG_STORAGE_KEY) as Language) || "CPP",
  );
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const reload = useCallback(() => {
    problemApi
      .get(slug)
      .then(setProblem)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load problem"))
      .finally(() => setLoading(false));
    // Samples load independently — a failure here must not block the statement.
    problemApi
      .samples(slug)
      .then(setSamples)
      .catch(() => setSamples([]));
  }, [slug]);

  useEffect(() => {
    setLoading(true);
    setError(null);
    setSamples([]);
    reload();
  }, [reload]);

  async function onSubmit() {
    if (!problem) return;
    if (!code.trim()) {
      setSubmitError("Source code cannot be empty.");
      return;
    }
    setSubmitError(null);
    setSubmitting(true);
    localStorage.setItem(LANG_STORAGE_KEY, language);
    try {
      const sub = await submissionApi.submit({
        problemId: problem.id,
        contestId,
        language,
        sourceCode: code,
      });
      navigate(`/submissions/${sub.id}`);
    } catch (e) {
      setSubmitError(e instanceof ApiError ? e.message : "Submission failed");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!problem) return null;

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_260px]">
      <div className="min-w-0 space-y-4">
        {contestId && (
          <div className="flex items-center justify-between rounded border border-cf-blue bg-blue-50 px-3 py-2 text-[12px]">
            <span className="font-bold text-cf-blue">Contest mode</span>
            <Link to={`/contests`} className="text-cf-blue hover:underline">
              submissions count toward standings → back to contest
            </Link>
          </div>
        )}
        {/* Statement panel */}
        <article className="cf-panel p-5">
          <header className="mb-4 border-b border-cf-border pb-3 text-center">
            <h1 className="text-[20px] font-bold">{problem.title}</h1>
            <div className="mt-1 text-[12px] text-gray-600">
              time limit: {problem.timeLimitMs / 1000}s &nbsp;|&nbsp; memory limit:{" "}
              {problem.memoryLimitMb} MB
            </div>
          </header>

          <div className="statement">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkMath]}
              rehypePlugins={[rehypeKatex]}
            >
              {problem.statement}
            </ReactMarkdown>
          </div>

          {problem.inputFormat && (
            <section className="mt-4">
              <h2 className="font-bold">Input</h2>
              <div className="statement">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm, remarkMath]}
                  rehypePlugins={[rehypeKatex]}
                >
                  {problem.inputFormat}
                </ReactMarkdown>
              </div>
            </section>
          )}

          {problem.outputFormat && (
            <section className="mt-4">
              <h2 className="font-bold">Output</h2>
              <div className="statement">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm, remarkMath]}
                  rehypePlugins={[rehypeKatex]}
                >
                  {problem.outputFormat}
                </ReactMarkdown>
              </div>
            </section>
          )}

          {samples.length > 0 && (
            <section className="mt-4">
              <h2 className="font-bold">Examples</h2>
              <div className="mt-2 space-y-3">
                {samples.map((s, i) => (
                  <div key={s.id} className="overflow-hidden rounded border border-cf-border">
                    <div className="grid grid-cols-1 md:grid-cols-2">
                      <div className="border-b border-cf-border md:border-b-0 md:border-r">
                        <div className="flex items-center justify-between bg-gray-50 px-3 py-1 text-[12px] font-bold">
                          <span>Input {samples.length > 1 ? `#${i + 1}` : ""}</span>
                          <button
                            type="button"
                            className="font-normal text-cf-blue hover:underline"
                            onClick={() => navigator.clipboard?.writeText(s.input)}
                          >
                            copy
                          </button>
                        </div>
                        <pre className="overflow-x-auto px-3 py-2 text-[13px] leading-tight">{s.input}</pre>
                      </div>
                      <div>
                        <div className="bg-gray-50 px-3 py-1 text-[12px] font-bold">Output</div>
                        <pre className="overflow-x-auto px-3 py-2 text-[13px] leading-tight">{s.output}</pre>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </article>

        {/* Submit panel */}
        <div className="cf-panel">
          <div className="cf-panel-title">Submit</div>
          <div className="p-3">
            {!user ? (
              <p className="text-gray-600">
                <Link to="/login" className="font-bold">
                  Enter
                </Link>{" "}
                to submit a solution.
              </p>
            ) : (
              <div className="space-y-2">
                {submitError && <ErrorBox message={submitError} />}
                <div className="flex items-center gap-2">
                  <label className="font-bold">Language:</label>
                  <select
                    className="cf-input w-auto"
                    value={language}
                    onChange={(e) => setLanguage(e.target.value as Language)}
                  >
                    {LANGUAGES.map((l) => (
                      <option key={l} value={l}>
                        {LANGUAGE_LABELS[l]}
                      </option>
                    ))}
                  </select>
                </div>
                <CodeEditor value={code} onChange={setCode} />
                <button
                  onClick={onSubmit}
                  disabled={submitting}
                  className="cf-btn cf-btn-primary"
                >
                  {submitting ? "Submitting…" : "Submit"}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Sidebar meta */}
      <aside className="space-y-4">
        {canSetProblems(user?.role) && (
          <ProblemAdminPanel problem={problem} onChanged={reload} />
        )}
        <div className="cf-panel">
          <div className="cf-panel-title">Problem info</div>
          <table className="w-full text-[12px]">
            <tbody>
              <tr className="border-b border-cf-border">
                <td className="px-3 py-1.5 font-bold text-gray-600">Difficulty</td>
                <td className="px-3 py-1.5">
                  <DifficultyBadge difficulty={problem.difficulty} />
                </td>
              </tr>
              <tr className="border-b border-cf-border">
                <td className="px-3 py-1.5 font-bold text-gray-600">Author</td>
                <td className="px-3 py-1.5">
                  <RatingName username={problem.authorUsername} />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-1.5 font-bold text-gray-600">Status</td>
                <td className="px-3 py-1.5">
                  <Link to={`/status?problemId=${problem.id}`}>All submissions →</Link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {problem.tags.length > 0 && (
          <div className="cf-panel">
            <div className="cf-panel-title">Tags</div>
            <div className="flex flex-wrap gap-1 p-2">
              {problem.tags.map((t) => (
                <Link
                  key={t}
                  to={`/problems?tag=${encodeURIComponent(t)}`}
                  className="rounded-sm border border-cf-border bg-cf-panel px-1.5 py-0.5 text-[11px] text-gray-700 no-underline hover:bg-gray-200"
                >
                  {t}
                </Link>
              ))}
            </div>
          </div>
        )}
      </aside>
    </div>
  );
}
