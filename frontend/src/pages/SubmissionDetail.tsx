import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { submissionApi } from "../lib/services";
import type { SubmissionDetail as Submission } from "../lib/types";
import { LANGUAGE_LABELS } from "../lib/types";
import { ApiError } from "../lib/api";
import { VerdictBadge } from "../components/VerdictBadge";
import { RatingName } from "../components/RatingName";
import { Spinner, ErrorBox } from "../components/Spinner";
import { formatDateTime, formatKb, formatMs, isPending } from "../lib/format";

const POLL_INTERVAL_MS = 1500;

export function SubmissionDetail() {
  const { id = "" } = useParams();
  const [sub, setSub] = useState<Submission | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const timer = useRef<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    const numId = Number(id);

    async function poll() {
      try {
        const data = await submissionApi.get(numId);
        if (cancelled) return;
        setSub(data);
        setLoading(false);
        if (isPending(data.verdict)) {
          timer.current = window.setTimeout(poll, POLL_INTERVAL_MS);
        }
      } catch (e) {
        if (cancelled) return;
        setError(e instanceof ApiError ? e.message : "Failed to load submission");
        setLoading(false);
      }
    }

    poll();
    return () => {
      cancelled = true;
      if (timer.current) window.clearTimeout(timer.current);
    };
  }, [id]);

  if (loading) return <Spinner label="Loading submission…" />;
  if (error) return <ErrorBox message={error} />;
  if (!sub) return null;

  return (
    <div className="space-y-4">
      <div className="cf-panel">
        <div className="cf-panel-title">Submission #{sub.id}</div>
        <table className="w-full text-[13px]">
          <tbody>
            <Row label="Problem">
              <span className="font-bold">{sub.problemTitle}</span>
              <Link to={`/status?problemId=${sub.problemId}`} className="ml-2 text-[12px]">
                (all submissions →)
              </Link>
            </Row>
            <Row label="Author">
              <RatingName username={sub.username} />
            </Row>
            <Row label="Language">{LANGUAGE_LABELS[sub.language] ?? sub.language}</Row>
            <Row label="Verdict">
              <VerdictBadge
                verdict={sub.verdict}
                passed={sub.testCasesPassed}
                total={sub.totalTestCases}
              />
              {sub.totalTestCases > 0 && (
                <span className="ml-2 text-gray-500">
                  ({sub.testCasesPassed}/{sub.totalTestCases} tests)
                </span>
              )}
            </Row>
            <Row label="Time">{formatMs(sub.timeUsedMs)}</Row>
            <Row label="Memory">{formatKb(sub.memoryUsedKb)}</Row>
            <Row label="Submitted">{formatDateTime(sub.submittedAt)}</Row>
            <Row label="Judged">{formatDateTime(sub.judgedAt)}</Row>
          </tbody>
        </table>
      </div>

      <div className="cf-panel">
        <div className="cf-panel-title">Source code</div>
        <pre className="overflow-x-auto p-3 font-mono text-[12px] leading-[1.5]">
          {sub.sourceCode}
        </pre>
      </div>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <tr className="border-b border-cf-border last:border-0">
      <td className="w-32 bg-cf-panel px-3 py-1.5 font-bold text-gray-600">{label}</td>
      <td className="px-3 py-1.5">{children}</td>
    </tr>
  );
}
